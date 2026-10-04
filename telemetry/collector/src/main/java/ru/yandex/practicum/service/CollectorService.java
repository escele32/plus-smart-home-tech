package ru.yandex.practicum.service;

import ru.yandex.practicum.model.hub.HubEvent;
import ru.yandex.practicum.model.sensor.SensorEvent;

public interface CollectorService {

    void collectorHubEvent(HubEvent hubEvent);

    void collectorSensorEvent(SensorEvent sensorEvent);
}
