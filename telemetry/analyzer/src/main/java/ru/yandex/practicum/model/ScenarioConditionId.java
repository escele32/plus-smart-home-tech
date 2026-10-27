package ru.yandex.practicum.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScenarioConditionId implements Serializable {
    Scenario scenario;
    Sensor sensor;
    Condition condition;
}
