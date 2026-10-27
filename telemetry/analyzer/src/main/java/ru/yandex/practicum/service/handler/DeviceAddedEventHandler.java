package ru.yandex.practicum.service.handler;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.model.Sensor;
import ru.yandex.practicum.repository.SensorRepository;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeviceAddedEventHandler implements HubEventHandler {
    SensorRepository sensorRepository;

    @Override
    public Class<? extends SpecificRecordBase> getPayloadType() {
        return DeviceAddedEventAvro.class;
    }

    @Override
    public void handle(HubEventAvro eventAvro) {
        DeviceAddedEventAvro payload = (DeviceAddedEventAvro) eventAvro.getPayload();
        if (sensorRepository.existsById(payload.getId())) {
            log.info("Датчик с id {} уже существует", payload.getId());
            return;
        }
        Sensor sensor = Sensor.builder()
                .id(payload.getId())
                .hubId(eventAvro.getHubId())
                .build();
        sensorRepository.save(sensor);
        log.info("Регистрация нового датчика с id {} в хабе с id {}", sensor.getId(), sensor.getHubId());
    }
}
