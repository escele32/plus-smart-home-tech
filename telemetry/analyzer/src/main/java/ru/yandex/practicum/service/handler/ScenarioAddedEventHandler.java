package ru.yandex.practicum.service.handler;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro;
import ru.yandex.practicum.model.*;
import ru.yandex.practicum.repository.*;

import java.util.Collection;

import static java.lang.String.format;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ScenarioAddedEventHandler implements HubEventHandler {
    ActionRepository actionRepository;
    ConditionRepository conditionRepository;
    ScenarioActionRepository scenarioActionRepository;
    ScenarioConditionRepository scenarioConditionRepository;
    ScenarioRepository scenarioRepository;
    SensorRepository sensorRepository;

    private Integer extractIntegerValue(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof Boolean bool) {
            return bool ? 1 : 0;
        }
        return (Integer) value;
    }

    private void cleanScenario(Long scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId).orElseThrow(() -> {
            log.error("Сценария с id {} не существует", scenarioId);
            throw new IllegalArgumentException(format("Сценария с id %d не существует", scenarioId));
        });
        Collection<ScenarioCondition> conditions = scenarioConditionRepository.findByScenarioId(scenarioId);
        Collection<ScenarioAction> actions = scenarioActionRepository.findByScenarioId(scenarioId);
        scenario.getConditions().clear();
        scenario.getActions().clear();
        scenarioConditionRepository.deleteByScenarioId(scenarioId);
        scenarioActionRepository.deleteByScenarioId(scenarioId);
        conditions.forEach(scenarioCondition ->
                conditionRepository.deleteById(scenarioCondition.getCondition().getId()));
        actions.forEach(scenarioAction ->
                actionRepository.deleteById(scenarioAction.getAction().getId()));
    }

    @Override
    public Class<? extends SpecificRecordBase> getPayloadType() {
        return ScenarioAddedEventAvro.class;
    }

    @Transactional
    @Override
    public void handle(HubEventAvro eventAvro) {
        ScenarioAddedEventAvro payload = (ScenarioAddedEventAvro) eventAvro.getPayload();
        String hubId = eventAvro.getHubId();

        Scenario scenario = scenarioRepository.findByHubIdAndName(hubId, payload.getName())
                .orElseGet(() -> scenarioRepository.save(
                        Scenario.builder().hubId(hubId).name(payload.getName()).build()));

        cleanScenario(scenario.getId());

        for (ScenarioConditionAvro conditionAvro : payload.getConditions()) {
            String sensorId = conditionAvro.getSensorId();
            if (sensorRepository.findByIdAndHubId(sensorId, hubId).isEmpty()) {
                log.warn("Датчик {} не зарегистрирован в хабе {}, условие сценария '{}' пропущено",
                        sensorId, hubId, payload.getName());
                continue;
            }

            Condition condition = conditionRepository.save(Condition.builder()
                    .type(ConditionType.valueOf(conditionAvro.getType().name()))
                    .operation(ConditionOperation.valueOf(conditionAvro.getOperation().name()))
                    .value(extractIntegerValue(conditionAvro.getValue()))
                    .build());

            scenarioConditionRepository.save(ScenarioCondition.builder()
                    .scenario(scenario)
                    .sensor(sensorRepository.findByIdAndHubId(sensorId, hubId).get())
                    .condition(condition)
                    .build());
        }

        for (DeviceActionAvro actionAvro : payload.getActions()) {
            String sensorId = actionAvro.getSensorId();
            if (sensorRepository.findByIdAndHubId(sensorId, hubId).isEmpty()) {
                log.warn("Датчик {} не зарегистрирован в хабе {}, действие сценария '{}' пропущено",
                        sensorId, hubId, payload.getName());
                continue;
            }

            Action action = actionRepository.save(Action.builder()
                    .type(ActionType.valueOf(actionAvro.getType().name()))
                    .value(extractIntegerValue(actionAvro.getValue()))
                    .build());

            scenarioActionRepository.save(ScenarioAction.builder()
                    .scenario(scenario)
                    .sensor(sensorRepository.findByIdAndHubId(sensorId, hubId).get())
                    .action(action)
                    .build());
        }

        log.info("Сценарий '{}' хаба {} сохранён", payload.getName(), hubId);
    }
}
