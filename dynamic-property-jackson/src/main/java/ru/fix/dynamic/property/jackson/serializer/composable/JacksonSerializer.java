package ru.fix.dynamic.property.jackson.serializer.composable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import ru.fix.dynamic.property.jackson.serializer.std.DurationSerializer;
import ru.fix.dynamic.property.jackson.serializer.std.PathSerializer;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class JacksonSerializer implements ComposableSerializer {

    private final ObjectMapper mapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerModule(new JavaTimeModule())
            .registerModule(new KotlinModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    public JacksonSerializer() {
        SimpleModule localDatetimeModule = new SimpleModule();
        localDatetimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        localDatetimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        SimpleModule durationModule = new SimpleModule();
        durationModule.addSerializer(Duration.class, new DurationSerializer());
        durationModule.addSerializer(Path.class, new PathSerializer());
        mapper.registerModules(localDatetimeModule, durationModule);
    }

    @Override
    public Optional<String> serialize(Object marshalledObject) throws IOException {
        return Optional.ofNullable(mapper.writeValueAsString(marshalledObject));
    }

    @Override
    public <T> Optional<T> deserialize(String rawString, Class<T> clazz) throws IOException {
        return Optional.ofNullable(mapper.readValue(rawString, clazz));
    }
}
