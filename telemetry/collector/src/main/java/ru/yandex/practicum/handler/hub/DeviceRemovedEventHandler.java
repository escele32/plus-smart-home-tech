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
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeviceRemovedEventHandler implements HubEventHandler {
    KafkaProducer producer;
    HubEventConverter converter;

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.DEVICE_REMOVED;
    }

    @Override
    public void handle(HubEventProto eventProto) {
        if (!eventProto.getPayloadCase().equals(getMessageType())) {
            log.warn("Хендлер DEVICE_REMOVED получил событие другого типа: {}", eventProto.getPayloadCase());
            return;
        }
        log.debug("Получено событие хаба: hubId={}, type={}",
                eventProto.getHubId(), eventProto.getPayloadCase());
        HubEventAvro eventAvro = converter.convert(eventProto);
        if (eventAvro == null) {
            log.warn("Конвертер вернул null для события hubId={}", eventProto.getHubId());
            return;
        }
        log.info("Устройство удалено: hubId={}, deviceId={}",
                eventProto.getHubId(),
                eventProto.getDeviceRemoved().getId());
        producer.sendHubEvent(eventAvro);
    }
}
