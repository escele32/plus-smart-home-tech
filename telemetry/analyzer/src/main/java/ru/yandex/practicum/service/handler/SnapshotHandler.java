package ru.yandex.practicum.service.handler;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.model.*;
import ru.yandex.practicum.repository.*;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SnapshotHandler {
    ActionRepository actionRepository;
    ConditionRepository conditionRepository;
    ScenarioActionRepository scenarioActionRepository;
    ScenarioConditionRepository scenarioConditionRepository;
    ScenarioRepository scenarioRepository;
    HubRouterEventDataService hubRouterService;
    ValueExtractor valueExtractor;

    private boolean isAccordance(ConditionOperation operation, int actualValue, Integer conditionValue) {
        if (conditionValue == null) {
            return false;
        }
        return switch (operation) {
            case EQUALS -> actualValue == conditionValue;
            case GREATER_THAN -> actualValue > conditionValue;
            case LOWER_THAN -> actualValue < conditionValue;
        };
    }

    private boolean isMeet(Scenario scenario, Map<String, SensorStateAvro> sensorsState) {
        List<ScenarioCondition> conditions = scenarioConditionRepository.findByScenarioId(scenario.getId())
                .stream().toList();
        if (conditions.isEmpty()) {
            return false;
        }
        for (ScenarioCondition scenarioCondition : conditions) {
            SensorStateAvro state = sensorsState.get(scenarioCondition.getSensor().getId());
            if (state == null) {
                return false;
            }
            Condition condition = conditionRepository.findById(scenarioCondition.getCondition().getId())
                    .orElse(null);
            if (condition == null) {
                return false;
            }
            SpecificRecordBase data = (SpecificRecordBase) state.getData();
            Integer actualValue = valueExtractor.extractValue(condition.getType(), data);
            if (actualValue == null || !isAccordance(condition.getOperation(), actualValue, condition.getValue())) {
                return false;
            }
        }
        return true;
    }

    private void executeActions(Scenario scenario, String hubId) {
        List<ScenarioAction> actions = scenarioActionRepository.findByScenarioId(scenario.getId()).stream().toList();
        for (ScenarioAction scenarioAction : actions) {
            Action action = actionRepository.findById(scenarioAction.getAction().getId()).orElse(null);
            if (action == null) {
                continue;
            }
            hubRouterService.send(hubId, scenario.getName(), scenarioAction.getSensor().getId(), action);
        }
    }

    public void handleSnapshot(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        Map<String, SensorStateAvro> sensorsState = snapshot.getSensorsState();

        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);
        for (Scenario scenario : scenarios) {
            if (isMeet(scenario, sensorsState)) {
                executeActions(scenario, hubId);
            }
        }
    }
}
