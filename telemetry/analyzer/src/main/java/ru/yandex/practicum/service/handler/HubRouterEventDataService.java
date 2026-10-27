package ru.yandex.practicum.service.handler;

import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.model.Action;

import java.time.Instant;

@Service
@Slf4j
public class HubRouterEventDataService {

    @GrpcClient("hub-router")
    private HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterStub;

    public void send(String hubId, String name, String sensorId, Action action) {
        DeviceActionProto.Builder actionProto = DeviceActionProto.newBuilder()
                .setSensorId(sensorId)
                .setType(ActionTypeProto.valueOf(action.getType().name()));
        if (action.getValue() != null) {
            actionProto.setValue(action.getValue());
        }
        Instant timestamp = Instant.now();
        DeviceActionRequest request = DeviceActionRequest.newBuilder()
                .setHubId(hubId)
                .setScenarioName(name)
                .setAction(actionProto.build())
                .setTimestamp(
                        Timestamp.newBuilder()
                                .setSeconds(timestamp.getEpochSecond())
                                .setNanos(timestamp.getNano())
                                .build()
                )
                .build();
        try {
            hubRouterStub.handleDeviceAction(request);
            log.info("Отправлено действие {} по датчику {} (сценарий '{}', хаб {})",
                    action.getType(), sensorId, name, hubId);
        } catch (Exception exception) {
            log.error("Не удалось отправить действие {} по датчику {} (сценарий '{}', хаб {})",
                    action.getType(), sensorId, name, hubId, exception);
        }
    }
}
