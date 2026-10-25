package ru.yandex.practicum.service;

import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.KafkaClient;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.service.handler.SnapshotHandler;
import ru.yandex.practicum.util.KafkaTopics;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SnapshotProcessor implements Runnable {
    KafkaClient kafkaClient;
    SnapshotHandler snapshotHandler;
    AtomicBoolean running = new AtomicBoolean(true);
    Consumer<String, SensorsSnapshotAvro> consumer;

    public SnapshotProcessor(KafkaClient kafkaClient, SnapshotHandler snapshotHandler) {
        this.kafkaClient = kafkaClient;
        this.snapshotHandler = snapshotHandler;
        this.consumer = kafkaClient.getSnapshotConsumer();
    }

    @Override
    public void run() {
        if (consumer == null) {
            log.error("Kafka client вернул null Consumer. Проверьте конфигурацию KafkaClient.");
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        consumer.subscribe(List.of(KafkaTopics.TELEMETRY_SNAPSHOTS_V1));
        log.info("Запуск SnapshotProcessor. Подписка на topic {}", KafkaTopics.TELEMETRY_SNAPSHOTS_V1);
        try {
            while (running.get()) {
                ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(Duration.ofMillis(200));
                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    try {
                        snapshotHandler.handleSnapshot(record.value());
                    } catch (Exception exception) {
                        log.error("Ошибка обработки снапшота хаба с id {}", record.value().getHubId(), exception);
                    }
                }

                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
            }
        } catch (WakeupException exception) {
            log.info("Получен WakeupException — инициировано завершение SnapshotProcessor");
        } catch (Exception exception) {
            log.error("Критическая ошибка во время обработки снапшотов", exception);
        } finally {
            log.info("Закрываем consumer");
            consumer.close();
        }
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        consumer.wakeup();
    }
}
