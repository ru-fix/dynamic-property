package ru.fix.dynamic.property.api.converter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import ru.fix.dynamic.property.api.converter.exception.DynamicPropertySerializationException;
import ru.fix.dynamic.property.api.converter.exception.DynamicPropertyDeserializationException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


/**
 * @author Ayrat Zulkarnyaev
 */
public class DefaultDynamicPropertyMarshaller implements DynamicPropertyMarshaller {

    private final ObjectMapper mapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerModule(new JavaTimeModule())
            .registerModule(new KotlinModule());

    public DefaultDynamicPropertyMarshaller() {
        SimpleModule localDatetimeModule = new SimpleModule();
        localDatetimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        localDatetimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        mapper.registerModule(localDatetimeModule);
    }

    @Override
    public String marshall(Object marshalledObject) {
        if (String.class.equals(marshalledObject.getClass())) {
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

        try {
            return mapper.readValue(rawString, clazz);
        } catch (IOException e) {
            throw new DynamicPropertyDeserializationException(
                    String.format("Failed to unmarshall json text to type %s. JSon: %s", clazz, rawString), e);
        }
    }

}
