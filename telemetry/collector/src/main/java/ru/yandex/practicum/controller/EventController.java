package ru.yandex.practicum.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.grpc.telemetry.collector.CollectorControllerGrpc;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.handler.hub.HubEventHandler;
import ru.yandex.practicum.handler.sensor.SensorEventHandler;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;

@GrpcService
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventController extends CollectorControllerGrpc.CollectorControllerImplBase {
    Map<SensorEventProto.PayloadCase, SensorEventHandler> sensorEventHandlers;
    Map<HubEventProto.PayloadCase, HubEventHandler> hubEventHandlers;

    public EventController(Set<SensorEventHandler> sensorEventHandlers, Set<HubEventHandler> hubEventHandlers) {
        // Преобразовываем набор хендлеров в map, где ключ — тип события от конкретного датчика или хаба.
        // Это нужно для упрощения поиска подходящего хендлера во время обработки событий
        this.sensorEventHandlers = sensorEventHandlers.stream()
                .collect(Collectors.toMap(
                        SensorEventHandler::getMessageType,
                        Function.identity()
                ));
        this.hubEventHandlers = hubEventHandlers.stream()
                .collect(Collectors.toMap(
                        HubEventHandler::getMessageType,
                        Function.identity()
                ));
    }

    @Override
    public void collectSensorEvent(SensorEventProto request, StreamObserver<Empty> responseObserver) {
        try {
            // проверяем, есть ли обработчик для полученного события
            if (sensorEventHandlers.containsKey(request.getPayloadCase())) {
                // если обработчик найден, передаём событие ему на обработку
                sensorEventHandlers.get(request.getPayloadCase()).handle(request);
            } else {
                throw new IllegalArgumentException(format("Не могу найти обработчик для события %s",
                        request.getPayloadCase()));
            }
            // после обработки события возвращаем ответ клиенту
            responseObserver.onNext(Empty.getDefaultInstance());
            // и завершаем обработку запроса
            responseObserver.onCompleted();
        } catch (Exception exception) {
            // в случае исключения отправляем ошибку клиенту
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(exception.getLocalizedMessage())
                            .withCause(exception)
            ));
        }
    }

    @Override
    public void collectHubEvent(HubEventProto request, StreamObserver<Empty> responseObserver) {
        try {
            if (hubEventHandlers.containsKey(request.getPayloadCase())) {
                hubEventHandlers.get(request.getPayloadCase()).handle(request);
            } else {
                throw new IllegalArgumentException(format("Не могу найти обработчик для события %s",
                        request.getPayloadCase()));
            }
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception exception) {
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(exception.getLocalizedMessage())
                            .withCause(exception)
            ));
        }
    }
}
