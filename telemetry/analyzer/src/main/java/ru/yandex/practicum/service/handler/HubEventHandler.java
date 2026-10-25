package ru.yandex.practicum.service.handler;

import org.apache.avro.specific.SpecificRecordBase;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

public interface HubEventHandler {

    Class<? extends SpecificRecordBase> getPayloadType();

    void handle(HubEventAvro eventAvro);
}
