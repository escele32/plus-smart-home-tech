package ru.yandex.practicum.handler.sensor;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.KafkaProducer;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.mapper.SensorEventConverter;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SwitchSensorEventHandler implements SensorEventHandler {
    KafkaProducer producer;
    SensorEventConverter converter;

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.SWITCH_SENSOR;
    }

    @Override
    public void handle(SensorEventProto eventProto) {
        if (!eventProto.getPayloadCase().equals(getMessageType())) {
            log.warn("Хендлер SWITCH_SENSOR получил событие другого типа: {}", eventProto.getPayloadCase());
            return;
        }
        log.debug("Получено событие сенсора: hubId={}, type={}",
                eventProto.getHubId(), eventProto.getPayloadCase());
        SensorEventAvro eventAvro = converter.convert(eventProto);
        if (eventAvro == null) {
            log.warn("Конвертер вернул null для события hubId={}", eventProto.getHubId());
            return;
        }
        log.info("Обработка переключателя: state={} датчика c id {}",
                eventProto.getSwitchSensor().getState() ? 1 : 0,
                eventProto.getId());
        producer.sendSensorEvent(eventAvro);
    }
}
