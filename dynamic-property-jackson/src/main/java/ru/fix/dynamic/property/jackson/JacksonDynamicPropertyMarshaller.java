package ru.fix.dynamic.property.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller;
import ru.fix.dynamic.property.api.marshaller.exception.DynamicPropertySerializationException;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


/**
 * Implementation of {@link DynamicPropertyMarshaller} which provides serialization and deserialization by Jacskon.
 *
 * @author Ayrat Zulkarnyaev
 */
public class JacksonDynamicPropertyMarshaller implements DynamicPropertyMarshaller {

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
        mapper.registerModules(localDatetimeModule, durationModule);

    }

    @Override
    public String marshall(Object marshalledObject) {
        if (String.class.equals(marshalledObject.getClass()) ||
                Duration.class.equals(marshalledObject.getClass())) {
            return marshalledObject.toString();
        }

        try {
            return mapper.writeValueAsString(marshalledObject);
        } catch (JsonProcessingException e) {
            throw new DynamicPropertySerializationException(
                    "Failed to marshalling pojo. Object details: " + marshalledObject, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unmarshall(String rawString, Class<T> clazz) {
        if (String.class.equals(clazz)) {
            return (T) rawString;
        }

        if (Duration.class.equals(clazz)) {
            return (T) Duration.parse(rawString);
        }

        try {
            return mapper.readValue(rawString, clazz);
        } catch (IOException e) {
            throw new DynamicPropertySerializationException(
                    String.format("Failed to unmarshall json text to type %s. JSon: %s", clazz, rawString), e);
        }
    }

}
