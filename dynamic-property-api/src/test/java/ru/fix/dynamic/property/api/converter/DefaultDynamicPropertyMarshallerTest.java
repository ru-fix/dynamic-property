package ru.fix.dynamic.property.api.converter;

import org.junit.jupiter.api.Test;
import ru.fix.dynamic.property.api.marshaller.DefaultDynamicPropertyMarshaller;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultDynamicPropertyMarshallerTest {

    private DefaultDynamicPropertyMarshaller marshaller = new DefaultDynamicPropertyMarshaller();

    @Test
    public void marshallAndUnmarshallPrimitiveTypes_shouldBeEqual() {
        String someString = "1";
        Integer someInteger = 1;
        Long someLong = 1L;
        Byte someByte = 1;
        BigDecimal someBigDecimal = new BigDecimal("1.1");
        BigInteger someBigInteger = BigInteger.valueOf(11L);
        Boolean someBoolean = false;
        Double someDouble = 1.1D;
        Float someFloat = 1.1F;

        assertPrimitives(someString, String.class);
        assertPrimitives(someInteger, Integer.class);
        assertPrimitives(someLong, Long.class);
        assertPrimitives(someByte, Byte.class);
        assertPrimitives(someBigDecimal, BigDecimal.class);
        assertPrimitives(someBigInteger, BigInteger.class);
        assertPrimitives(someBoolean, Boolean.class);
        assertPrimitives(someDouble, Double.class);
        assertPrimitives(someFloat, Float.class);
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

        User user = new User("pretty name", new Email("test@mail.ua"), phones);

        String serialized = marshaller.marshall(user);
        User deserialized = marshaller.unmarshall(serialized, User.class);

        assertEquals(user.getName(), deserialized.getName());
        assertEquals(user.getEmail().getValue(), deserialized.getEmail().getValue());
    }

}