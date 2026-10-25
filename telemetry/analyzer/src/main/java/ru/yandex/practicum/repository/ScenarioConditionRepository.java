package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.model.ScenarioCondition;
import ru.yandex.practicum.model.ScenarioConditionId;

import java.util.Collection;

public interface ScenarioConditionRepository extends JpaRepository<ScenarioCondition, ScenarioConditionId> {

    void deleteBySensorId(String sensorId);

    void deleteByScenarioId(Long scenarioId);

    Collection<ScenarioCondition> findByScenarioId(Long scenarioId);
}
