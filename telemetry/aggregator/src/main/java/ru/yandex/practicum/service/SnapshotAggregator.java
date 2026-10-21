package ru.yandex.practicum.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.mapper.SensorStateConverter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SnapshotAggregator {
    Map<String, SensorsSnapshotAvro> snapshots = new ConcurrentHashMap<>();
    SensorStateConverter converter;

    public Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        String hubId = event.getHubId();
        String deviceId = event.getId();

        SensorsSnapshotAvro snapshot = snapshots.computeIfAbsent(hubId, k -> {
            SensorsSnapshotAvro sensorsSnapshot = new SensorsSnapshotAvro();
            sensorsSnapshot.setHubId(hubId);
            sensorsSnapshot.setTimestamp(event.getTimestamp());
            sensorsSnapshot.setSensorsState(new HashMap<>());
            return sensorsSnapshot;
        });

        Map<String, SensorStateAvro> states = snapshot.getSensorsState();
        SensorStateAvro currentState = states.get(deviceId);

        SensorStateAvro newState = converter.toSensorState(event);
        Instant eventTs = event.getTimestamp();

        if (currentState != null) {
            Instant currentTs = currentState.getTimestamp();
            // Если старое событие новее или равно — игнорируем
            if (currentTs != null && currentTs.compareTo(eventTs) >= 0) {
                return Optional.empty();
            }
            // Если данные не изменились — тоже не обновляем снапшот
            if (Objects.equals(currentState.getData(), newState.getData())) {
                // Можно обновить timestamp состояния, но сам снапшот писать не нужно
                currentState.setTimestamp(eventTs);
                // Обновляем timestamp снапшота, если он был null или старее
                if (snapshot.getTimestamp() == null || snapshot.getTimestamp().compareTo(eventTs) < 0) {
                    snapshot.setTimestamp(eventTs);
                }
                return Optional.empty();
            }
        }

        // Обновляем состояние датчика
        states.put(deviceId, newState);

        // Вычисляем новый timestamp снапшота: max(старый, eventTs)
        Instant newSnapshotTs;
        Instant existingSnapshotTs = snapshot.getTimestamp();
        if (existingSnapshotTs == null) {
            newSnapshotTs = eventTs;
        } else {
            newSnapshotTs = (existingSnapshotTs.compareTo(eventTs) >= 0)
                    ? existingSnapshotTs
                    : eventTs;
        }
        snapshot.setTimestamp(newSnapshotTs);

        return Optional.of(snapshot);
    }

    // Опционально: если нужно получить снапшот без обновления
    public SensorsSnapshotAvro getSnapshot(String hubId) {
        return snapshots.get(hubId);
    }
}
