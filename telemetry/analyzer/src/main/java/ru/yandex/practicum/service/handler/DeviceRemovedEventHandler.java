package ru.yandex.practicum.service.handler;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.repository.ScenarioActionRepository;
import ru.yandex.practicum.repository.ScenarioConditionRepository;
import ru.yandex.practicum.repository.SensorRepository;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeviceRemovedEventHandler implements HubEventHandler {
    SensorRepository sensorRepository;
    ScenarioActionRepository scenarioActionRepository;
    ScenarioConditionRepository scenarioConditionRepository;

    @Override
    public Class<? extends SpecificRecordBase> getPayloadType() {
        return DeviceRemovedEventAvro.class;
    }

    @Override
    public void handle(HubEventAvro eventAvro) {
        DeviceRemovedEventAvro payload = (DeviceRemovedEventAvro) eventAvro.getPayload();
        if (sensorRepository.findByIdAndHubId(payload.getId(), eventAvro.getHubId()).isEmpty()) {
            log.info("Датчик с id {} не найден в хабе с id {}", payload.getId(), eventAvro.getHubId());
            return;
        }
        scenarioConditionRepository.deleteBySensorId(payload.getId());
        scenarioActionRepository.deleteBySensorId(payload.getId());
        sensorRepository.deleteByIdAndHubId(payload.getId(), eventAvro.getHubId());
        log.info("Датчик с id {} удалён из хаба с id {}", payload.getId(), eventAvro.getHubId());
    }
}
