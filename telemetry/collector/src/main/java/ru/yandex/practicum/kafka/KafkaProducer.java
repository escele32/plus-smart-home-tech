package ru.yandex.practicum.kafka;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.util.KafkaTopics;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class KafkaProducer {
    KafkaClient kafkaClient;

    private void send(String topic, String key, Long timestamp, SpecificRecordBase data) {
        ProducerRecord<String, SpecificRecordBase> producerRecord =
                new ProducerRecord<>(topic, null, timestamp, key, data);
        kafkaClient.getProducer().send(producerRecord, (metadata, exception) -> {
            if (exception != null) {
                log.error("Не удалось отправить событие в Kafka topic={}, key={}", topic, key, exception);
            } else {
                log.debug("Событие отправлено в Kafka topic={}, partition={}, offset={}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }

    public void sendHubEvent(HubEventAvro eventAvro) {
        send(KafkaTopics.TELEMETRY_HUBS_V1, eventAvro.getHubId(), eventAvro.getTimestamp().toEpochMilli(), eventAvro);
    }

    public void sendSensorEvent(SensorEventAvro eventAvro) {
        send(KafkaTopics.TELEMETRY_SENSORS_V1, eventAvro.getHubId(), eventAvro.getTimestamp().toEpochMilli(), eventAvro);
    }
}
