package ru.yandex.practicum.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.Properties;

@Configuration
public class KafkaClientConfiguration {

    @Bean
    @Scope("prototype")
    KafkaClient getClient() {
        return new KafkaClient() {
            private Producer<String, SensorsSnapshotAvro> producer;
            private Consumer<String, SensorEventAvro> consumer;

            private void initProducer() {
                Properties config = new Properties();
                config.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
                config.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                        "org.apache.kafka.common.serialization.StringSerializer");
                config.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                        "ru.yandex.practicum.kafka.AvroSerializer");
                producer = new KafkaProducer<>(config);
            }

            private void initConsumer() {
                Properties config = new Properties();
                config.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
                config.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                        "org.apache.kafka.common.serialization.StringDeserializer");
                config.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                        "ru.yandex.practicum.kafka.SensorEventDeserializer");
                config.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "aggregator-service");
                config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
                consumer = new KafkaConsumer<>(config);
            }

            @Override
            public Producer<String, SensorsSnapshotAvro> getProducer() {
                if (producer == null) {
                    initProducer();
                }
                return producer;
            }

            @Override
            public Consumer<String, SensorEventAvro> getConsumer() {
                if (consumer == null) {
                    initConsumer();
                }
                return consumer;
            }

            @Override
            public void stop() {
                if (producer != null) {
                    producer.close();
                }
                if (consumer != null) {
                    consumer.close();
                }
            }
        };
    }
}
