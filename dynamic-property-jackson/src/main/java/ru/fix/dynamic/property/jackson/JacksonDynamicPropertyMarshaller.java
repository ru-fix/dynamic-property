package ru.fix.dynamic.property.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller;
import ru.fix.dynamic.property.api.marshaller.exception.DynamicPropertySerializationException;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;


/**
 * Implementation of {@link DynamicPropertyMarshaller} which provides serialization and deserialization by Jacskon.
 *
 */
public class JacksonDynamicPropertyMarshaller implements DynamicPropertyMarshaller {

    private final StdSerializer stdSerializer = new StdSerializer();

    private final ObjectMapper mapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerModule(new JavaTimeModule())
            .registerModule(new KotlinModule());

    public JacksonDynamicPropertyMarshaller() {
        SimpleModule localDatetimeModule = new SimpleModule();
        localDatetimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        localDatetimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        SimpleModule durationModule = new SimpleModule();
        durationModule.addSerializer(Duration.class, new DurationSerializer());
        durationModule.addSerializer(Path.class, new PathSerializer());
        mapper.registerModules(localDatetimeModule, durationModule);

    }

    @Override
    public String marshall(Object marshalledObject) {
        Objects.requireNonNull(marshalledObject);
        try {
            Optional<String> result = stdSerializer.marshall(marshalledObject);
            if(result.isPresent()){
                return result.get();
            }

            return mapper.writeValueAsString(marshalledObject);
        } catch (Exception e) {
            throw new DynamicPropertySerializationException(
                    "Failed to serialize. Type: " + marshalledObject.getClass() + ". Instance: " + marshalledObject, e);
        }
    }

    @Override
    public <T> T unmarshall(String rawString, Class<T> type) {
        Objects.requireNonNull(rawString);
        Objects.requireNonNull(type);
        try {
            Optional<Object> result = stdSerializer.unmarshall(rawString, type);
            if (result.isPresent()) {
                return (T) result.get();
            }

            return mapper.readValue(rawString, type);
        } catch (Exception e) {
            throw new DynamicPropertySerializationException(
                    "Failed to deserialize. Type: " + type + ". From " + rawString, e);
        }
    }
}
