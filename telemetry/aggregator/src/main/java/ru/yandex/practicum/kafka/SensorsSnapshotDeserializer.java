package ru.yandex.practicum.kafka;

import org.apache.avro.Schema;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

public class SensorsSnapshotDeserializer extends BaseAvroDeserializer<SensorsSnapshotAvro> {
    public SensorsSnapshotDeserializer(Schema schema) {

        super(SensorsSnapshotAvro.getClassSchema());
    }
}
