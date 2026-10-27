package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "scenario_conditions")
@IdClass(ScenarioConditionId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScenarioCondition {
    @Id
    @ManyToOne
    @JoinColumn(name = "scenario_id")
    Scenario scenario;

    @Id
    @ManyToOne
    @JoinColumn(name = "sensor_id")
    Sensor sensor;

    @Id
    @ManyToOne
    @JoinColumn(name = "condition_id")
    Condition condition;
}