package ru.yandex.practicum.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;

import static java.lang.String.format;

@Slf4j
@Component
public class SensorStateConverter {

    @SuppressWarnings("unchecked")
    public SensorStateAvro toSensorState(SensorEventAvro event) {
        SensorStateAvro state = new SensorStateAvro();
        state.setTimestamp(event.getTimestamp());

        Object payload = event.getPayload();
        if (payload == null) {
            log.warn("Событие с id={} равно null", event.getId());
            return null;
        }

        if (payload instanceof ClimateSensorAvro) {
            state.setData(payload);
        } else if (payload instanceof LightSensorAvro) {
            state.setData(payload);
        } else if (payload instanceof MotionSensorAvro) {
            state.setData(payload);
        } else if (payload instanceof SwitchSensorAvro) {
            state.setData(payload);
        } else if (payload instanceof TemperatureSensorAvro) {
            state.setData(payload);
        } else {
            throw new IllegalArgumentException(format("Неизвестный тип SensorEventAvro: %s",
                    payload.getClass().getName()));
        }

        return state;
    }
}
