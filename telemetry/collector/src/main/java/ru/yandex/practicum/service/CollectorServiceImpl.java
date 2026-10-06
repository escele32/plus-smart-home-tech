package ru.yandex.practicum.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.KafkaProducer;
import ru.yandex.practicum.mapper.HubMapper;
import ru.yandex.practicum.mapper.SensorMapper;
import ru.yandex.practicum.model.hub.HubEvent;
import ru.yandex.practicum.model.sensor.SensorEvent;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CollectorServiceImpl implements CollectorService {
    KafkaProducer kafkaProducer;

    @Override
    public void collectorHubEvent(HubEvent hubEvent) {
        kafkaProducer.sendHubEvent(HubMapper.mapToHubAvro(hubEvent));
    }

    @Override
    public void collectorSensorEvent(SensorEvent sensorEvent) {
        kafkaProducer.sendSensorEvent(SensorMapper.mapToSensorAvro(sensorEvent));
    }
}
