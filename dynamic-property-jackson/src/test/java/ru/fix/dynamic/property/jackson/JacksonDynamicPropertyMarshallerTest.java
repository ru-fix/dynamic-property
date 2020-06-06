package ru.fix.dynamic.property.jackson;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JacksonDynamicPropertyMarshallerTest {

    private final JacksonDynamicPropertyMarshaller marshaller = new JacksonDynamicPropertyMarshaller();

    @Test
    public void marshallPrimitiveTypes() {
        assertPrimitivesMarshalling("1", "1");
        assertPrimitivesMarshalling(1, "1");
        assertPrimitivesMarshalling(1L, "1");
        assertPrimitivesMarshalling(Byte.parseByte("1"), "1");
        assertPrimitivesMarshalling(new BigDecimal("1.1"), "1.1");
        assertPrimitivesMarshalling(BigInteger.valueOf(11L), "11");
        assertPrimitivesMarshalling(false, "false");
        assertPrimitivesMarshalling(true, "true");
        assertPrimitivesMarshalling(1.1D, "1.1");
        assertPrimitivesMarshalling(1.F, "1.0");
        assertPrimitivesMarshalling(Duration.ofMinutes(1), "PT1M");
        assertPrimitivesMarshalling(Paths.get("my-file.txt"), "my-file.txt");
        assertPrimitivesMarshalling(
                Paths.get("my-file.txt").toAbsolutePath(),
                Paths.get("my-file.txt").toAbsolutePath().toString());
    }

    private void assertPrimitivesMarshalling(Object object, String target) {
        String value = marshaller.marshall(object);
        assertEquals(target, value);
    }

    @Test
    public void unmarshallPrimitiveTypes() {
        assertPrimitivesUnmarshalling("1", "1");
        assertPrimitivesUnmarshalling("1", 1);
        assertPrimitivesUnmarshalling("1", 1L);
        assertPrimitivesUnmarshalling("1", (byte) 1);
        assertPrimitivesUnmarshalling("1.1", new BigDecimal("1.1"));
        assertPrimitivesUnmarshalling("11", BigInteger.valueOf(11L));
        assertPrimitivesUnmarshalling("false", false);
        assertPrimitivesUnmarshalling("true", true);
        assertPrimitivesUnmarshalling("1.1", 1.1D);
        assertPrimitivesUnmarshalling("1.1", 1.1F);
        assertPrimitivesUnmarshalling("PT1M", Duration.ofMinutes(1));
        assertPrimitivesUnmarshalling("my-file.txt", Paths.get("my-file.txt"));
        assertPrimitivesUnmarshalling("/some/dir/my-file.txt", Paths.get("/some/dir/my-file.txt"));
    }

    private void assertPrimitivesUnmarshalling(String source, Object target) {
        Object value = marshaller.unmarshall(source, target.getClass());
        assertEquals(target, value);
    }

    @Test
    public void marshallAndUnmarshallPrimitiveTypes_shouldBeEqual() {
        assertPrimitivesMarshallAndUnmarshall("1", String.class);
        assertPrimitivesMarshallAndUnmarshall(1, Integer.class);
        assertPrimitivesMarshallAndUnmarshall(1L, Long.class);
        assertPrimitivesMarshallAndUnmarshall(Byte.parseByte("1"), Byte.class);
        assertPrimitivesMarshallAndUnmarshall(new BigDecimal("1.1"), BigDecimal.class);
        assertPrimitivesMarshallAndUnmarshall(BigInteger.valueOf(11L), BigInteger.class);
        assertPrimitivesMarshallAndUnmarshall(false, Boolean.class);
        assertPrimitivesMarshallAndUnmarshall(true, Boolean.class);
        assertPrimitivesMarshallAndUnmarshall(1.1D, Double.class);
        assertPrimitivesMarshallAndUnmarshall(1.F, Float.class);
        assertPrimitivesMarshallAndUnmarshall(Duration.ofMinutes(1), Duration.class);
        assertPrimitivesMarshallAndUnmarshall(Paths.get("my-file.txt"), Path.class);
        assertPrimitivesMarshallAndUnmarshall(Paths.get("my-file.txt").toAbsolutePath(), Path.class);
    }

    private <T> void assertPrimitivesMarshallAndUnmarshall(T sourceValue, Class<T> clazz) {
        String serialize = marshaller.marshall(sourceValue);
        T deserialize = marshaller.unmarshall(serialize, clazz);
        assertEquals(sourceValue, deserialize);
    }


    private static final String USER_JSON = "" +
            "{" +
            "\"name\":\"pretty name\"," +
            "\"email\":{" +
            "\"value\":\"test@mail.ua\"" +
            "}," +
            "\"phones\":[88005553535,11111111111]," +
            "\"sessionDuration\":\"PT1S\"" +
            "}";


    private static final String USER_JSON_WITH_UNKNOWN_FIELD = "" +
            "{" +
            "\"name\":\"pretty name\"," +
            "\"email\":{" +
            "\"value\":\"test@mail.ua\"" +
            "}," +
            "\"phones\":[88005553535,11111111111]," +
            "\"sessionDuration\":\"PT1S\"," +
            "\"unknownField\":\"123\"" +
            "}";

    private static final User USER = new User("pretty name", new Email("test@mail.ua"),
            Arrays.asList(BigInteger.valueOf(88005553535L), BigInteger.valueOf(11111111111L)), Duration.ofSeconds(1));

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


    @Test
    public void marshal_and_unmarshal_relative_path_inside_complex_object() {
        PathContainer obj = new PathContainer(Paths.get("location"));
        String serialized = marshaller.marshall(obj);
        assertEquals("{\"path\":\"location\"}", serialized);

        PathContainer deserialized = marshaller.unmarshall(serialized, PathContainer.class);
        assertEquals(obj.getPath(), deserialized.getPath());
    }

    @Test
    public void marshal_and_unmarshal_absolute_path_inside_complex_object() {
        PathContainer obj = new PathContainer(Paths.get("/some/location"));
        String serialized = marshaller.marshall(obj);
        assertEquals("{\"path\":\"/some/location\"}", serialized);

        PathContainer deserialized = marshaller.unmarshall(serialized, PathContainer.class);
        assertEquals(obj.getPath(), deserialized.getPath());
    }

    @Test
    public void unmarshal_object_with_unknown_field() {
        User deserialized = marshaller.unmarshall(USER_JSON_WITH_UNKNOWN_FIELD, User.class);
        assertEquals(USER.getName(), deserialized.getName());
        assertEquals(USER.getEmail().getValue(), deserialized.getEmail().getValue());
        assertEquals(USER.getSessionDuration(), deserialized.getSessionDuration());
        assertEquals(USER.getPhones(), deserialized.getPhones());
    }

}