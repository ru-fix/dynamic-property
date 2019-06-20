package ru.fix.dynamic.property.api.converter;

/**
 * @author Ayrat Zulkarnyaev
 */
public interface DynamicPropertyMarshaller {

    String marshall(Object marshalledObject) throws DynamicPropertySerializationException;

    <T> T unmarshall(String rawString, Class<T> clazz) throws DynamicPropertyDeserializationException;

}
