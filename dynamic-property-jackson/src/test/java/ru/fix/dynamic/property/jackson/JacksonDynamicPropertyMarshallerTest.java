package ru.fix.dynamic.property.jackson;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JacksonDynamicPropertyMarshallerTest {
    private JacksonDynamicPropertyMarshaller marshaller = new JacksonDynamicPropertyMarshaller();

    private static final String USER_JSON = "{\"name\":\"pretty name\",\"email\":{\"value\":\"test@mail.ua\"}," +
            "\"phones\":[88005553535,11111111111],\"sessionDuration\":\"PT1S\"}";

    private static final User USER = new User("pretty name", new Email("test@mail.ua"),
            Arrays.asList(BigInteger.valueOf(88005553535L), BigInteger.valueOf(11111111111L)), Duration.ofSeconds(1));


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


    @Test
    public void unmarshallPrimitiveTypesFromString_shouldBeEqual() {

        System.out.println(Duration.ofMinutes(1));

        assertPrimitivesFromString("1", "1");
        assertPrimitivesFromString("1", 1);
        assertPrimitivesFromString("1", 1L);
        assertPrimitivesFromString("1", (byte) 1);
        assertPrimitivesFromString("1.1", new BigDecimal("1.1"));
        assertPrimitivesFromString("11", BigInteger.valueOf(11L));
        assertPrimitivesFromString("false", false);
        assertPrimitivesFromString("true", true);
        assertPrimitivesFromString("1.1", 1.1D);
        assertPrimitivesFromString("1.1", 1.1F);
        assertPrimitivesFromString("PT1M", Duration.ofMinutes(1));

    }


    private void assertPrimitivesFromString(String source, Object target) {
        Object value = marshaller.unmarshall(source, target.getClass());
        assertEquals(target, value);
    }

    private <T> void assertPrimitives(T sourceValue, Class<T> clazz) {
        String serialize = marshaller.marshall(sourceValue);
        T deserialize = marshaller.unmarshall(serialize, clazz);

        assertEquals(sourceValue, deserialize);
    }

    @Test
    public void marshallAndUnmarshallComplexObject_shouldBeEqual() {

        String serialized = marshaller.marshall(USER);
        User deserialized = marshaller.unmarshall(serialized, User.class);

        assertEquals(USER_JSON, serialized);
        assertEquals(USER.getName(), deserialized.getName());
        assertEquals(USER.getEmail().getValue(), deserialized.getEmail().getValue());
        assertEquals(USER.getSessionDuration(), deserialized.getSessionDuration());
        assertEquals(USER.getPhones(), deserialized.getPhones());
    }

    @Test
    public void unmarshallComplexObject_shouldBeEquals() {
        User deserialized = marshaller.unmarshall(USER_JSON, User.class);
        assertEquals(USER.getName(), deserialized.getName());
        assertEquals(USER.getEmail().getValue(), deserialized.getEmail().getValue());
        assertEquals(USER.getSessionDuration(), deserialized.getSessionDuration());
        assertEquals(USER.getPhones(), deserialized.getPhones());
    }
}