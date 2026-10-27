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
        if (event == null || event.getHubId() == null || event.getId() == null) {
            log.warn("Получено некорректное событие: hubId={}, id={}",
                    event != null ? event.getHubId() : null,
                    event != null ? event.getId() : null);
            return Optional.empty();
        }

        String hubId = event.getHubId();
        String deviceId = event.getId();
        Instant eventTimestamp = event.getTimestamp();

        boolean[] changed = {false};

        SensorsSnapshotAvro updated = snapshots.compute(hubId, (key, current) -> {
            Map<String, SensorStateAvro> states;

            if (current == null || current.getSensorsState() == null) {
                states = new HashMap<>();
            } else {
                // Безопасное копирование
                states = new HashMap<>(current.getSensorsState());
            }

            SensorStateAvro previousState = states.get(deviceId);

            // Защита от устаревших событий
            if (previousState != null
                    && previousState.getTimestamp() != null
                    && eventTimestamp.isBefore(previousState.getTimestamp())) {
                return current;
            }

            SensorStateAvro newState;
            try {
                newState = converter.toSensorState(event);
            } catch (IllegalArgumentException e) {
                log.error("Ошибка конвертации события для hubId={}, deviceId={}: {}",
                        hubId, deviceId, e.getMessage(), e);
                return current; // Пропускаем это событие, но не ломаем агрегацию
            }

            if (newState == null) {
                return current;
            }

            if (previousState != null && previousState.equals(newState)) {
                return current;
            }

            states.put(deviceId, newState);

            SensorsSnapshotAvro snapshot = new SensorsSnapshotAvro();
            snapshot.setHubId(hubId);
            snapshot.setTimestamp(eventTimestamp);
            snapshot.setSensorsState(states);

            changed[0] = true;
            return snapshot;
        });

        return changed[0] ? Optional.of(updated) : Optional.empty();
    }
}
