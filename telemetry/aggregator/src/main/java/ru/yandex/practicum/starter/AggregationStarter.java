package ru.yandex.practicum.starter;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;

import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.service.SnapshotAggregator;
import ru.yandex.practicum.kafka.KafkaClient;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AggregationStarter {

    KafkaClient kafkaClient;
    SnapshotAggregator aggregator;

    /**
     * Метод для начала процесса агрегации данных.
     * Подписывается на топик telemetry.sensors.v1, читает события,
     * формирует снапшоты и отправляет их в telemetry.snapshots.v1.
     */
    public void start() {
        Consumer<String, SensorEventAvro> consumer = kafkaClient.getConsumer();
        Producer<String, SensorsSnapshotAvro> producer = kafkaClient.getProducer();

        // Подписываемся на нужный топик
        consumer.subscribe(List.of("telemetry.sensors.v1"));
        log.info("Aggregator подписан на топик telemetry.sensors.v1");

        try {
            while (true) {
                // Poll с таймаутом 100 мс
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    SensorEventAvro event = record.value();
                    if (event == null) {
                        continue;
                    }

                    // Обновляем снапшот по событию
                    Optional<SensorsSnapshotAvro> updated = aggregator.updateState(event);
                    if (!updated.isPresent()) {
                        // Состояние не изменилось — ничего не отправляем
                        continue;
                    }

                    SensorsSnapshotAvro snapshot = updated.get();
                    String key = snapshot.getHubId(); // ключ — hubId

                    ProducerRecord<String, SensorsSnapshotAvro> pr =
                            new ProducerRecord<>("telemetry.snapshots.v1", key, snapshot);

                    producer.send(pr, (metadata, exception) -> {
                        if (exception != null) {
                            log.error("Не удалось отправить снапшот для hubId={}", key, exception);
                        } else {
                            log.debug("Снапшот отправлен: hubId={}, offset={}", key, metadata.offset());
                        }
                    });
                }

                // Ручной коммит оффсетов (если в KafkaConfig auto-commit=false)
                consumer.commitSync();
            }
        } catch (WakeupException ignored) {
            log.info("Получен WakeupException — инициировано завершение агрегатора");
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            try {
                // Сначала flush продюсера — чтобы все сообщения ушли в Kafka
                producer.flush();
                log.info("Буфер продюсера сброшен (flush)");

                // Потом коммит оставшихся оффсетов
                consumer.commitSync();
                log.info("Оффсеты зафиксированы (commitSync)");
            } catch (Exception flushOrCommitEx) {
                log.warn("Ошибка при flush продюсера или commit консьюмера", flushOrCommitEx);
            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
                log.info("Закрываем продюсер");
                producer.close();
            }
        }
    }
}

