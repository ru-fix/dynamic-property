package ru.fix.dynamic.property.jackson;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller;

import java.nio.file.Path;
import java.util.Optional;

import static ru.fix.dynamic.property.jackson.MarshallerBuilder.InternalMarshaller;

public class CustomMarshallerTest {

    private EnumMarshaller enumMarshaller;
    private LongMarshaller longMarshaller;

    private DynamicPropertyMarshaller marshaller;

    @BeforeEach
    public void before() {
        enumMarshaller = Mockito.spy(new EnumMarshaller());
        longMarshaller = Mockito.spy(new LongMarshaller());
        marshaller = MarshallerBuilder.newBuilder()
                .addMarshaller(enumMarshaller)
                .addMarshaller(longMarshaller)
                .build();
    }

    @Test
    public void marshall_and_unmarshall_enum() {
        UserRole enumVal = UserRole.USER;
        String stringVal = marshaller.marshall(enumVal);
        marshaller.unmarshall(stringVal, UserRole.class);

        Mockito.verify(enumMarshaller, Mockito.times(1)).marshall(enumVal);
        Mockito.verify(enumMarshaller, Mockito.times(1)).unmarshall(stringVal, UserRole.class);
    }

    @Test
    public void std_serializer_before_custom_marshaller() {
        Long longValue = 5L;
        String stringVal = marshaller.marshall(longValue);
        marshaller.unmarshall(stringVal, Long.class);

        Mockito.verify(longMarshaller, Mockito.never()).marshall(longValue);
        Mockito.verify(longMarshaller, Mockito.never()).unmarshall(stringVal, Long.class);
    }

    private enum UserRole {
        ADMIN,
        USER
    }

    private static class EnumMarshaller implements InternalMarshaller {

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

    private static class LongMarshaller implements InternalMarshaller {

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
