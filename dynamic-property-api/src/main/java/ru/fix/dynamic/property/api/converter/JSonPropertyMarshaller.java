package ru.fix.dynamic.property.api.converter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.module.kotlin.KotlinModule;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


//TODO: попробовать так чтобы работало без дополнительного конвертера
/**
 * @author Ayrat Zulkarnyaev
 */
public class JSonPropertyMarshaller implements DynamicPropertyMarshaller {

    private final ObjectMapper mapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerModule(new JavaTimeModule())
            .registerModule(new KotlinModule());

    public JSonPropertyMarshaller() {
        SimpleModule localDatetimeModule = new SimpleModule();
        localDatetimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        localDatetimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        mapper.registerModule(localDatetimeModule);
    }

    @Override
    public String marshall(Object marshalledObject) {
        try {
            return mapper.writeValueAsString(marshalledObject);
        } catch (JsonProcessingException e) {
            throw new DynamicPropertySerializationException(
                    "Failed to marshalling pojo. Object details: " + marshalledObject, e);
        }
    }

    @Override
    public <T> T unmarshall(String rawString, Class<T> clazz) {
        try {
            return mapper.readValue(rawString, clazz);
        } catch (IOException e) {
            throw new DynamicPropertyDeserializationException(
                    String.format("Failed to unmarshall json text to type %s. JSon: %s", clazz, rawString), e);
        }
    }

}
