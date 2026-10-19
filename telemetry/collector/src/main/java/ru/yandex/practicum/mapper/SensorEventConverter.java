package ru.yandex.practicum.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;

import static java.lang.String.format;

@Component
@Slf4j
public class SensorEventConverter {

    public SensorEventAvro convert(SensorEventProto eventProto) {
        if (eventProto == null) {
            return null;
        }
        SensorEventAvro.Builder builder = SensorEventAvro.newBuilder()
                .setId(eventProto.getId())
                .setHubId(eventProto.getHubId())
                .setTimestamp(Instant.ofEpochSecond(
                        eventProto.getTimestamp().getSeconds(),
                        eventProto.getTimestamp().getNanos()));
        switch (eventProto.getPayloadCase()) {
            case CLIMATE_SENSOR -> {
                builder.setPayload(ClimateSensorAvro.newBuilder()
                        .setTemperatureC(eventProto.getClimateSensor().getTemperatureC())
                        .setHumidity(eventProto.getClimateSensor().getHumidity())
                        .setCo2Level(eventProto.getClimateSensor().getCo2Level())
                        .build());
            }
            case LIGHT_SENSOR -> {
                builder.setPayload(LightSensorAvro.newBuilder()
                        .setLinkQuality(eventProto.getLightSensor().getLinkQuality())
                        .setLuminosity(eventProto.getLightSensor().getLuminosity())
                        .build());
            }
            case MOTION_SENSOR -> {
                builder.setPayload(MotionSensorAvro.newBuilder()
                        .setLinkQuality(eventProto.getMotionSensor().getLinkQuality())
                        .setMotion(eventProto.getMotionSensor().getMotion())
                        .setVoltage(eventProto.getMotionSensor().getVoltage())
                        .build());
            }
            case SWITCH_SENSOR -> {
                builder.setPayload(SwitchSensorAvro.newBuilder()
                        .setState(eventProto.getSwitchSensor().getState())
                        .build());
            }
            case TEMPERATURE_SENSOR -> {
                builder.setPayload(TemperatureSensorAvro.newBuilder()
                        .setTemperatureC(eventProto.getTemperatureSensor().getTemperatureC())
                        .setTemperatureF(eventProto.getTemperatureSensor().getTemperatureF())
                        .build());
            }
            default -> {
                throw new IllegalArgumentException(format("Неизвестный тип события датчика %s",
                    eventProto.getPayloadCase()));
            }
        }
        return builder.build();
    }
}
