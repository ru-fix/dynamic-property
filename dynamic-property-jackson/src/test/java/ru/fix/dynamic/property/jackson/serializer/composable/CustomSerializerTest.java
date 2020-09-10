package ru.fix.dynamic.property.jackson.serializer.composable;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller;
import ru.fix.dynamic.property.jackson.MarshallerBuilder;

import java.nio.file.Path;
import java.util.Optional;

public class CustomSerializerTest {

    private EnumSerializer enumSerializer;
    private LongSerializer longSerializer;

    private DynamicPropertyMarshaller marshaller;

    @BeforeEach
    public void before() {
        enumSerializer = Mockito.spy(new EnumSerializer());
        longSerializer = Mockito.spy(new LongSerializer());
        marshaller = MarshallerBuilder.newBuilder()
                .addSerializer(enumSerializer)
                .addSerializer(longSerializer)
                .build();
    }

    @Test
    public void marshall_and_unmarshall_enum() {
        UserRole enumVal = UserRole.USER;
        String stringVal = marshaller.marshall(enumVal);
        marshaller.unmarshall(stringVal, UserRole.class);

        Mockito.verify(enumSerializer, Mockito.times(1)).serialize(enumVal);
        Mockito.verify(enumSerializer, Mockito.times(1)).deserialize(stringVal, UserRole.class);
    }

    @Test
    public void std_serializer_before_custom_marshaller() {
        Long longValue = 5L;
        String stringVal = marshaller.marshall(longValue);
        marshaller.unmarshall(stringVal, Long.class);

        Mockito.verify(longSerializer, Mockito.never()).serialize(longValue);
        Mockito.verify(longSerializer, Mockito.never()).deserialize(stringVal, Long.class);
    }

    private enum UserRole {
        ADMIN,
        USER
    }

    private static class EnumSerializer implements ComposableSerializer {

        @Override
        public Optional<String> serialize(Object marshalledObject) {
            if (marshalledObject.getClass().isEnum()) {
                return Optional.of(((Enum) marshalledObject).name());
            }
            return Optional.empty();
        }

        @Override
        public <T> Optional<T> deserialize(String rawString, Class<T> clazz) {
            if (clazz.isEnum()) {
                for (T enumConstant : clazz.getEnumConstants()) {
                    if (((Enum) enumConstant).name().equalsIgnoreCase(rawString)) {
                        return Optional.of(enumConstant);
                    }
                }
            }
            return Optional.empty();
        }
    }

    private static class LongSerializer implements ComposableSerializer {

        @Override
        public Optional<String> serialize(Object marshalledObject) {
            if (Path.class.isAssignableFrom(marshalledObject.getClass())) {
                return Optional.of("custom long marshalling");
            }
            return Optional.empty();
        }

        @Override
        public <T> Optional<T> deserialize(String rawString, Class<T> clazz) {
            return Optional.empty();
        }
    }

}
