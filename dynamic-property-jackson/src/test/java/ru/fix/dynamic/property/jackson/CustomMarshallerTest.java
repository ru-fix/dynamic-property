package ru.fix.dynamic.property.jackson;


import org.junit.jupiter.api.Test;
import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.fix.dynamic.property.jackson.MarshallerBuilder.InternalMarshaller;
import static ru.fix.dynamic.property.jackson.MarshallerBuilder.newBuilder;

public class CustomMarshallerTest {

    private final DynamicPropertyMarshaller marshaller = newBuilder()
            .addMarshaller(new EnumMarshaller())
            .addMarshaller(new LongMarshaller())
            .build();

    @Test
    public void marshall_and_unmarshall_enum() {
        UserRole enumVal = UserRole.USER;
        String stringVal = marshaller.marshall(enumVal);

        UserRole unmarshall = marshaller.unmarshall(stringVal, UserRole.class);
        assertEquals(enumVal, unmarshall);
    }

    class EnumMarshaller implements InternalMarshaller {

        @Override
        public Optional<String> marshall(Object marshalledObject) {
            if (marshalledObject.getClass().isEnum()) {
                return Optional.of(((Enum) marshalledObject).name());
            }
            return Optional.empty();
        }

        @Override
        public <T> Optional<T> unmarshall(String rawString, Class<T> clazz) {
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

    enum UserRole {
        ADMIN,
        USER
    }

    @Test
    public void std_serializer_before_custom_marshaller() {
        Long longValue = 5L;
        assertEquals(marshaller.marshall(longValue), longValue.toString());
    }

    class LongMarshaller implements InternalMarshaller {

        @Override
        public Optional<String> marshall(Object marshalledObject) {
            if (Path.class.isAssignableFrom(marshalledObject.getClass())) {
                return Optional.of("custom long marshalling");
            }
            return Optional.empty();
        }

        @Override
        public <T> Optional<T> unmarshall(String rawString, Class<T> clazz) {
            return Optional.empty();
        }
    }

}
