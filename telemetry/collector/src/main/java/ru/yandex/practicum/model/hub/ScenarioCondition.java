package ru.yandex.practicum.model.hub;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScenarioCondition {
    @NotBlank
    String sensorId;
    @NotNull
    @JsonProperty("type")
    ConditionType conditionType;
    @NotNull
    @JsonProperty("operation")
    ConditionOperation conditionOperation;
    Object value;
}
