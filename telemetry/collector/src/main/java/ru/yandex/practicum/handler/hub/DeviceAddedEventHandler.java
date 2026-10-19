package ru.yandex.practicum.handler.hub;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.kafka.KafkaProducer;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.mapper.HubEventConverter;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class DeviceAddedEventHandler implements HubEventHandler {
    KafkaProducer producer;
    HubEventConverter converter;

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.DEVICE_ADDED;
    }

    @Override
    public void handle(HubEventProto eventProto) {
        if (!eventProto.getPayloadCase().equals(getMessageType())) {
            log.warn("Хендлер DEVICE_ADDED получил событие другого типа: {}", eventProto.getPayloadCase());
            return;
        }
        log.debug("Получено событие хаба: hubId={}, type={}",
                eventProto.getHubId(), eventProto.getPayloadCase());
        HubEventAvro eventAvro = converter.convert(eventProto);
        if (eventAvro == null) {
            log.warn("Конвертер вернул null для события hubId={}", eventProto.getHubId());
            return;
        }
        log.info("Устройство добавлено: hubId={}, deviceId={}, type={}",
                eventProto.getHubId(),
                eventProto.getDeviceAdded().getId(),
                eventProto.getDeviceAdded().getType().name());
        producer.sendHubEvent(eventAvro);
    }
}
