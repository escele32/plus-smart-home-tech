package ru.yandex.practicum.service.handler;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.model.ConditionType;

@Component
@Slf4j
@NoArgsConstructor
public class ValueExtractor {

    private Integer extractTemperature(SpecificRecordBase data) {
        if (data instanceof ClimateSensorAvro climateSensor) {
            return climateSensor.getTemperatureC();
        } else if (data instanceof TemperatureSensorAvro temperatureSensor) {
            return temperatureSensor.getTemperatureC();
        } else {
            return null;
        }
    }

    public Integer extractValue(ConditionType type, SpecificRecordBase data) {
        return switch (type) {
            case SWITCH -> data instanceof SwitchSensorAvro switchSensor ? (switchSensor.getState() ? 1 : 0) : null;
            case CO2LEVEL -> data instanceof ClimateSensorAvro climateSensor ? climateSensor.getCo2Level() : null;
            case TEMPERATURE -> extractTemperature(data);
            case HUMIDITY -> data instanceof ClimateSensorAvro climateSensor ? climateSensor.getHumidity() : null;
            case LUMINOSITY -> data instanceof LightSensorAvro lightSensor ? lightSensor.getLuminosity() : null;
            case MOTION -> data instanceof MotionSensorAvro motionSensor ? (motionSensor.getMotion() ? 1 : 0) : null;
            default -> throw new IllegalArgumentException("Не существующий тип датчика, либо список датчиков пуст");
        };
    }
}
