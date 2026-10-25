package ru.yandex.practicum.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.Properties;

@Configuration
public class KafkaClientConfiguration {

    @Bean
    @Scope("prototype")
    KafkaClient getClient() {
        return new KafkaClient() {
            private Consumer<String, SensorsSnapshotAvro> snapshotConsumer;
            private Consumer<String, HubEventAvro> hubConsumer;

            private void initSnapshotConsumer() {
                Properties config = new Properties();
                config.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
                config.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                        "org.apache.kafka.common.serialization.StringDeserializer");
                config.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                        "ru.yandex.practicum.kafka.SensorsSnapshotAvroDeserializer");
                config.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "analyzer-snapshot-service");
                config.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
                config.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, String.valueOf(100));
                snapshotConsumer = new KafkaConsumer<>(config);
            }

            private void initHubConsumer() {
                Properties config = new Properties();
                config.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
                config.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                        "org.apache.kafka.common.serialization.StringDeserializer");
                config.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                        "ru.yandex.practicum.kafka.HubEventAvroDeserializer");
                config.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "analyzer-hub-event-service");
                config.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
                config.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, String.valueOf(100));
                hubConsumer = new KafkaConsumer<>(config);
            }

            @Override
            public Consumer<String, SensorsSnapshotAvro> getSnapshotConsumer() {
                if (snapshotConsumer == null) {
                    initSnapshotConsumer();
                }
                return snapshotConsumer;
            }

            @Override
            public Consumer<String, HubEventAvro> getHubConsumer() {
                if (hubConsumer == null) {
                    initHubConsumer();
                }
                return hubConsumer;
            }

            @Override
            public void stop() {
                if (snapshotConsumer != null) {
                    snapshotConsumer.close();
                }
                if (hubConsumer != null) {
                    hubConsumer.close();
                }
            }
        };
    }
}
