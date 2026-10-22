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

    public void start() {
        Consumer<String, SensorEventAvro> consumer = kafkaClient.getConsumer();
        Producer<String, SensorsSnapshotAvro> producer = kafkaClient.getProducer();

        if (consumer == null || producer == null) {
            log.error("Kafka client вернул null Consumer или Producer. Проверьте конфигурацию KafkaClient.");
            return;
        }

        consumer.subscribe(List.of("telemetry.sensors.v1"));
        log.info("Aggregator подписан на топик telemetry.sensors.v1");

        try {
            while (true) {
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(Duration.ofMillis(200));

                int processedCount = 0;
                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    SensorEventAvro event = record.value();
                    if (event == null) {
                        log.warn("Пропущена запись с пустым событием: partition={}, offset={}",
                                record.partition(), record.offset());
                        continue;
                    }

                    Optional<SensorsSnapshotAvro> updated = aggregator.updateState(event);
                    if (updated.isEmpty()) {
                        continue;
                    }

                    SensorsSnapshotAvro snapshot = updated.get();
                    String key = snapshot.getHubId();
                    if (key == null) {
                        log.warn("Снапшот имеет null hubId, пропускаем отправку: offset={}", record.offset());
                        continue;
                    }

                    ProducerRecord<String, SensorsSnapshotAvro> output =
                            new ProducerRecord<>("telemetry.snapshots.v1", key, snapshot);

                    producer.send(output, (metadata, exception) -> {
                        if (exception != null) {
                            log.error("Не удалось отправить снапшот для hubId={}, offset потребителя={}",
                                    key, record.offset(), exception);
                            // Здесь можно добавить логику повторной попытки или dead-letter
                        } else {
                            log.debug("Снапшот отправлен: hubId={}, partition={}, offset={}",
                                    key, metadata.partition(), metadata.offset());
                        }
                    });

                    processedCount++;
                }

                if (!records.isEmpty()) {
                    consumer.commitSync();
                    log.trace("Коммит оффсетов после обработки {} записей", processedCount);
                }
            }
        } catch (WakeupException ignored) {
            log.info("Получен WakeupException — инициировано завершение агрегатора");
        } catch (Exception exception) {
            log.error("Критическая ошибка во время обработки событий от датчиков", exception);
        } finally {
            shutdownSafely(consumer, producer);
        }
    }

    private void shutdownSafely(Consumer<?, ?> consumer, Producer<?, ?> producer) {
        try {
            if (producer != null) {
                producer.flush();
                log.info("Буфер продюсера сброшен (flush)");
            }
        } catch (Exception exception) {
            log.warn("Ошибка при flush продюсера", exception);
        }

        try {
            if (consumer != null) {
                consumer.commitSync();
                log.info("Оффсеты зафиксированы (commitSync)");
                consumer.close();
                log.info("Консьюмер закрыт");
            }
        } catch (Exception exception) {
            log.warn("Ошибка при commit/close консьюмера", exception);
        }

        try {
            if (producer != null) {
                producer.close();
                log.info("Продюсер закрыт");
            }
        } catch (Exception exception) {
            log.warn("Ошибка при закрытии продюсера", exception);
        }
    }
}

