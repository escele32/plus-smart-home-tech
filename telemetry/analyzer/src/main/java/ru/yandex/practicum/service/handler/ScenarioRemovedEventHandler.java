package ru.yandex.practicum.service.handler;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro;
import ru.yandex.practicum.model.Scenario;
import ru.yandex.practicum.model.ScenarioAction;
import ru.yandex.practicum.model.ScenarioCondition;
import ru.yandex.practicum.repository.*;

import java.util.Collection;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ScenarioRemovedEventHandler implements HubEventHandler {
    ActionRepository actionRepository;
    ConditionRepository conditionRepository;
    ScenarioActionRepository scenarioActionRepository;
    ScenarioConditionRepository scenarioConditionRepository;
    ScenarioRepository scenarioRepository;

    @Override
    public Class<? extends SpecificRecordBase> getPayloadType() {
        return ScenarioRemovedEventAvro.class;
    }

    @Override
    public void handle(HubEventAvro eventAvro) {
        ScenarioRemovedEventAvro payload = (ScenarioRemovedEventAvro) eventAvro.getPayload();
        if (scenarioRepository.findByHubIdAndName(eventAvro.getHubId(), payload.getName()).isEmpty()) {
            log.info("Сценарий '{}' для хаба с id {} отсутствует", payload.getName(), eventAvro.getHubId());
            return;
        }
        Scenario scenario = scenarioRepository.findByHubIdAndName(eventAvro.getHubId(), payload.getName()).get();
        Collection<ScenarioCondition> conditions = scenarioConditionRepository.findByScenarioId(scenario.getId());
        Collection<ScenarioAction> actions = scenarioActionRepository.findByScenarioId(scenario.getId());
        scenarioConditionRepository.deleteByScenarioId(scenario.getId());
        scenarioActionRepository.deleteByScenarioId(scenario.getId());
        conditions.forEach(scenarioCondition ->
                conditionRepository.deleteById(scenarioCondition.getCondition().getId()));
        actions.forEach(scenarioAction ->
                actionRepository.deleteById(scenarioAction.getAction().getId()));
        scenarioRepository.delete(scenario);
        log.info("Сценарий '{}' для хаба с id {} удалён", payload.getName(), eventAvro.getHubId());
    }
}
