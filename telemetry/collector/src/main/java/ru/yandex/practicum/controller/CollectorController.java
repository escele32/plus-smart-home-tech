package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.model.hub.HubEvent;
import ru.yandex.practicum.model.sensor.SensorEvent;
import ru.yandex.practicum.service.CollectorService;
import ru.yandex.practicum.util.ApiPath;

@RestController
@RequestMapping(ApiPath.EVENTS)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CollectorController {
    CollectorService collectorService;

    @PostMapping(ApiPath.HUBS)
    public void collectorHubEvent(@RequestBody @Valid HubEvent hubEvent) {
        collectorService.collectorHubEvent(hubEvent);
    }

    @PostMapping(ApiPath.SENSOR)
    public void collectorSensorEvent(@RequestBody @Valid SensorEvent sensorEvent) {
        collectorService.collectorSensorEvent(sensorEvent);
    }
}
