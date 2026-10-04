package ru.yandex.practicum.kafka;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;


import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static java.lang.String.format;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class GeneralAvroSerializer implements Serializer<SpecificRecordBase> {
    final EncoderFactory encoderFactory = EncoderFactory.get();
    BinaryEncoder encoder;

    @Override
    public byte[] serialize(String topic, SpecificRecordBase data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] result = null;
            encoder = encoderFactory.binaryEncoder(out, encoder);
            if (data != null) {
                DatumWriter<SpecificRecordBase> writer = new SpecificDatumWriter<>(data.getSchema());
                writer.write(data, encoder);
                encoder.flush();
                result = out.toByteArray();
            }
            return result;
        } catch (IOException exception) {
            throw new SerializationException(format("Ошибка сериализации данных для топика %s", topic), exception);
        }
    }
}
