package ru.yandex.practicum.mapper;

import lombok.experimental.UtilityClass;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.model.sensor.*;

@UtilityClass
public class SensorMapper {

    private ClimateSensorAvro mapClimate(ClimateSensor event) {
        return ClimateSensorAvro.newBuilder()
                .setTemperatureC(event.getTemperatureC())
                .setHumidity(event.getHumidity())
                .setCo2Level(event.getCo2Level())
                .build();
    }

    private LightSensorAvro mapLight(LightSensor event) {
        return LightSensorAvro.newBuilder()
                .setLinkQuality(event.getLinkQuality())
                .setLuminosity(event.getLuminosity())
                .build();
    }

    private MotionSensorAvro mapMotion(MotionSensor event) {
        return MotionSensorAvro.newBuilder()
                .setLinkQuality(event.getLinkQuality())
                .setMotion(event.getMotion())
                .setVoltage(event.getVoltage())
                .build();
    }

    private SwitchSensorAvro mapSwitch(SwitchSensor event) {
        return SwitchSensorAvro.newBuilder()
                .setState(event.getState())
                .build();
    }

    private TemperatureSensorAvro mapTemperature(TemperatureSensor event) {
        return TemperatureSensorAvro.newBuilder()
                .setTemperatureC(event.getTemperatureC())
                .setTemperatureF(event.getTemperatureF())
                .build();
    }

    private Object mapPayload(SensorEvent event) {
        return switch (event.getType()) {
            case CLIMATE_SENSOR_EVENT -> mapClimate((ClimateSensor) event);
            case LIGHT_SENSOR_EVENT -> mapLight((LightSensor) event);
            case MOTION_SENSOR_EVENT -> mapMotion((MotionSensor) event);
            case SWITCH_SENSOR_EVENT -> mapSwitch((SwitchSensor) event);
            case TEMPERATURE_SENSOR_EVENT -> mapTemperature((TemperatureSensor) event);
        };
    }

    public SensorEventAvro mapToAvro(SensorEvent event) {
        return SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp())
                .setPayload(mapPayload(event))
                .build();
    }
}
