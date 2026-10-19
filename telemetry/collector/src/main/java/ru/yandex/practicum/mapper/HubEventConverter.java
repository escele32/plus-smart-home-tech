package ru.yandex.practicum.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

@Component
@Slf4j
public class HubEventConverter {

    private DeviceTypeAvro mapDeviceTypeAvro(DeviceTypeProto typeProto) {
        return switch (typeProto) {
            case LIGHT_SENSOR -> DeviceTypeAvro.LIGHT_SENSOR;
            case CLIMATE_SENSOR -> DeviceTypeAvro.CLIMATE_SENSOR;
            case MOTION_SENSOR -> DeviceTypeAvro.MOTION_SENSOR;
            case SWITCH_SENSOR -> DeviceTypeAvro.SWITCH_SENSOR;
            case TEMPERATURE_SENSOR -> DeviceTypeAvro.TEMPERATURE_SENSOR;
            default -> throw new IllegalArgumentException(format("Неизвестный тип датчика %s", typeProto));
        };
    }

    private ConditionTypeAvro mapConditionTypeAvro(ConditionTypeProto conditionTypeProto) {
        return switch (conditionTypeProto) {
            case MOTION -> ConditionTypeAvro.MOTION;
            case LUMINOSITY -> ConditionTypeAvro.LUMINOSITY;
            case HUMIDITY -> ConditionTypeAvro.HUMIDITY;
            case TEMPERATURE -> ConditionTypeAvro.TEMPERATURE;
            case CO2LEVEL -> ConditionTypeAvro.CO2LEVEL;
            case SWITCH -> ConditionTypeAvro.SWITCH;
            default -> throw new IllegalArgumentException(format("Неизвестный тип состояния %s", conditionTypeProto));
        };
    }

    private ConditionOperationAvro mapConditionOperationAvro(ConditionOperationProto conditionOperation) {
        return switch (conditionOperation) {
            case EQUALS -> ConditionOperationAvro.EQUALS;
            case LOWER_THAN -> ConditionOperationAvro.LOWER_THAN;
            case GREATER_THAN -> ConditionOperationAvro.GREATER_THAN;
            default -> throw new IllegalArgumentException(format("Неизвестный тип состояния операции %s",
                    conditionOperation));
        };
    }

    private ActionTypeAvro mapActionTypeAvro(ActionTypeProto typeProto) {
        return switch (typeProto) {
            case INVERSE -> ActionTypeAvro.INVERSE;
            case ACTIVATE -> ActionTypeAvro.ACTIVATE;
            case DEACTIVATE -> ActionTypeAvro.DEACTIVATE;
            case SET_VALUE -> ActionTypeAvro.SET_VALUE;
            default -> throw new IllegalArgumentException(format("Неизвестный тип действия %s", typeProto));
        };
    }

    private List<DeviceActionAvro> mapDeviceActionAvro(List<DeviceActionProto> protoList) {
        List<DeviceActionAvro> result = new ArrayList<>(protoList.size());
        for (DeviceActionProto proto : protoList) {
            DeviceActionAvro.Builder builder = DeviceActionAvro.newBuilder()
                    .setSensorId(proto.getSensorId())
                    .setType(mapActionTypeAvro(proto.getType()))
                    .setValue(proto.hasValue() ? proto.getValue() : null);
            result.add(builder.build());
        }
        return result;
    }

    private List<ScenarioConditionAvro> mapScenarioConditionAvro(List<ScenarioConditionProto> protoList) {
        List<ScenarioConditionAvro> result = new ArrayList<>(protoList.size());
        for (ScenarioConditionProto proto : protoList) {
            ScenarioConditionAvro.Builder builder = ScenarioConditionAvro.newBuilder()
                    .setSensorId(proto.getSensorId())
                    .setType(mapConditionTypeAvro(proto.getType()))
                    .setOperation(mapConditionOperationAvro(proto.getOperation()))
                    .setValue(switch (proto.getValueCase()) {
                        case INT_VALUE -> proto.getIntValue();
                        case BOOL_VALUE -> proto.getBoolValue();
                        default -> null;
                    });
            result.add(builder.build());
        }
        return result;
    }

    public HubEventAvro convert(HubEventProto eventProto) {
        if (eventProto == null) {
            return null;
        }
        HubEventAvro.Builder builder = HubEventAvro.newBuilder()
                .setHubId(eventProto.getHubId())
                .setTimestamp(Instant.ofEpochSecond(
                        eventProto.getTimestamp().getSeconds(),
                        eventProto.getTimestamp().getNanos()));
        switch (eventProto.getPayloadCase()) {
            case DEVICE_ADDED -> {
                builder.setPayload(DeviceAddedEventAvro.newBuilder()
                        .setId(eventProto.getDeviceAdded().getId())
                        .setType(mapDeviceTypeAvro(eventProto.getDeviceAdded().getType()))
                        .build());
            }
            case DEVICE_REMOVED -> {
                builder.setPayload(DeviceRemovedEventAvro.newBuilder()
                        .setId(eventProto.getDeviceAdded().getId())
                        .build());
            }
            case SCENARIO_ADDED -> {
                builder.setPayload(ScenarioAddedEventAvro.newBuilder()
                        .setName(eventProto.getScenarioAdded().getName())
                        .setConditions(mapScenarioConditionAvro(eventProto.getScenarioAdded().getConditionList()))
                        .setActions(mapDeviceActionAvro(eventProto.getScenarioAdded().getActionList()))
                        .build());
            }
            case SCENARIO_REMOVED -> {
                builder.setPayload(ScenarioRemovedEventAvro.newBuilder()
                        .setName(eventProto.getScenarioRemoved().getName())
                        .build());
            }
            default -> {
                throw new IllegalArgumentException(format("Неизвестный тип события хаба %s",
                        eventProto.getPayloadCase()));
            }
        }
        return builder.build();
    }
}
