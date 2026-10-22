package ru.yandex.practicum.kafka;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Deserializer;
import ru.yandex.practicum.exception.DeserializationException;

import static java.lang.String.format;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BaseAvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {
    DecoderFactory decoderFactory;
    SpecificDatumReader<T> reader;

    public BaseAvroDeserializer(Schema schema) {
        this(DecoderFactory.get(), schema);
    }

    public BaseAvroDeserializer(DecoderFactory decoderFactory, Schema schema) {
        this.decoderFactory = decoderFactory;
        this.reader = new SpecificDatumReader<>(schema);
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        try {
            if (data != null) {
                BinaryDecoder decoder = decoderFactory.binaryDecoder(data, null);
                return this.reader.read(null, decoder);
            }
            return null;
        } catch (Exception exception) {
            throw new DeserializationException(format("Ошибка десериализации данных из топика %s", topic), exception);
        }
    }
}
