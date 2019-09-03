package ru.fix.dynamic.property.jackson;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JacksonDynamicPropertyMarshallerTest {
    private JacksonDynamicPropertyMarshaller marshaller = new JacksonDynamicPropertyMarshaller();

    @Test
    public void marshallAndUnmarshallPrimitiveTypes_shouldBeEqual() {
        assertPrimitives("1", String.class);
        assertPrimitives(1, Integer.class);
        assertPrimitives(1L, Long.class);
        assertPrimitives(new Byte("1"), Byte.class);
        assertPrimitives(new BigDecimal("1.1"), BigDecimal.class);
        assertPrimitives(BigInteger.valueOf(11L), BigInteger.class);
        assertPrimitives(false, Boolean.class);
        assertPrimitives(true, Boolean.class);
        assertPrimitives(1.1D, Double.class);
        assertPrimitives(1.F, Float.class);
        assertPrimitives(Duration.ofMinutes(1), Duration.class);
    }

    private <T> void assertPrimitives(T sourceValue, Class<T> clazz) {
        String serialize = marshaller.marshall(sourceValue);
        T deserialize = marshaller.unmarshall(serialize, clazz);

        assertEquals(sourceValue, deserialize);
    }

    @Test
    public void marshallAndUnmarshallComplexObject_shouldBeEqual() {
        List<BigInteger> phones = new ArrayList<>();
        phones.add(BigInteger.valueOf(88005553535L));
        phones.add(BigInteger.valueOf(11111111111L));

        User user = new User("pretty name", new Email("test@mail.ua"), phones, Duration.ofSeconds(1));

        String serialized = marshaller.marshall(user);
        User deserialized = marshaller.unmarshall(serialized, User.class);

        assertEquals(user.getName(), deserialized.getName());
        assertEquals(user.getEmail().getValue(), deserialized.getEmail().getValue());
        assertEquals(user.getSessionDuration(), deserialized.getSessionDuration());
    }
}