package ru.yandex.practicum.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

public interface KafkaClient {

    Producer<String, SensorsSnapshotAvro> getProducer();

    Consumer<String, SensorEventAvro> getConsumer();

    void stop();
}
