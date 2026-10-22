package ru.yandex.practicum.service;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.collector.CollectorControllerGrpc;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.grpc.telemetry.event.TemperatureSensorProto;
import ru.yandex.practicum.model.sensor.TemperatureSensor;

import java.time.Instant;

@Component
@Slf4j
public class EventDataProducer {

    @GrpcClient("collector")
    private CollectorControllerGrpc.CollectorControllerBlockingStub collectorStub;

    private int getRandomSensorValue(int prevValue) {
        return (int) (prevValue + (Math.random() * 3 - 2));
    }

    private SensorEventProto createTemperatureSensorEvent(TemperatureSensor sensor) {
        int temperatureCelsius = getRandomSensorValue(sensor.getTemperatureC());
        int temperatureFahrenheit = (int) (temperatureCelsius * 1.8 + 32);
        Instant ts = Instant.now();

        return SensorEventProto.newBuilder()
                .setId(sensor.getId())
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(ts.getEpochSecond())
                        .setNanos(ts.getNano())
                ).setTemperatureSensor(
                        TemperatureSensorProto.newBuilder()
                                .setTemperatureC(temperatureCelsius)
                                .setTemperatureF(temperatureFahrenheit)
                                .build()
                )
                .build();
    }

    private void sendEvent(SensorEventProto event) {
        log.info("Отправляю данные: {}", event.getAllFields());
        Empty response = collectorStub.collectSensorEvent(event);
        log.info("Получил ответ от коллектора: {}", response);
    }
}