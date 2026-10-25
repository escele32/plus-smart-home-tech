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
import ru.yandex.practicum.service.handler.HubEventHandler;
import ru.yandex.practicum.kafka.KafkaClient;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.util.KafkaTopics;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HubEventProcessor implements Runnable {
    KafkaClient kafkaClient;
    AtomicBoolean running = new AtomicBoolean(true);
    Consumer<String, HubEventAvro> consumer;
    Collection<HubEventHandler> handlerCollection;

    public HubEventProcessor(KafkaClient kafkaClient, Collection<HubEventHandler> handlerCollection) {
        this.kafkaClient = kafkaClient;
        this.handlerCollection = handlerCollection;
        this.consumer = kafkaClient.getHubConsumer();
    }

    @Override
    public void run() {
        Consumer<String, HubEventAvro> consumer = kafkaClient.getHubConsumer();
        if (consumer == null) {
            log.error("Kafka client вернул null Consumer. Проверьте конфигурацию KafkaClient.");
            return;
        }
        Map<Class<?>, HubEventHandler> handlerMap = handlerCollection.stream()
                        .collect(Collectors.toMap(HubEventHandler::getPayloadType, Function.identity()));
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        consumer.subscribe(List.of(KafkaTopics.TELEMETRY_HUBS_V1));
        log.info("Запуск HubEventProcessor. Подписка на topic {}", KafkaTopics.TELEMETRY_HUBS_V1);
        try {
            while (running.get()) {
                ConsumerRecords<String, HubEventAvro> records = consumer.poll(Duration.ofMillis(200));
                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    HubEventAvro event = record.value();
                    try {
                        HubEventHandler handler = handlerMap.get(event.getPayload().getClass());
                        if (handler == null) {
                            log.info("Обработчик для события хаба c id {} не найден: {}",
                                    event.getHubId(), event.getPayload().getClass());
                            continue;
                        }
                        handler.handle(event);
                    } catch (Exception exception) {
                        log.error("Ошибка обработки события хаба c id {}", event.getHubId(), exception);
                    }
                }

                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
            }
        } catch (WakeupException exception) {
            log.info("Получен WakeupException — инициировано завершение HubEventProcessor");
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
