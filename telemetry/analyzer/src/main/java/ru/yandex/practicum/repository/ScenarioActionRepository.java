package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.model.ScenarioAction;
import ru.yandex.practicum.model.ScenarioActionId;

import java.util.Collection;

public interface ScenarioActionRepository extends JpaRepository<ScenarioAction, ScenarioActionId> {

    void deleteBySensorId(String sensorId);

    void deleteByScenarioId(Long scenarioId);

    Collection<ScenarioAction> findByScenarioId(Long scenarioId);
}
