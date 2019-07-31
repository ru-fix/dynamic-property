package ru.fix.dynamic.property.api.marshaller;

import ru.fix.dynamic.property.api.marshaller.exception.DynamicPropertySerializationException;

/**
 * @author Ayrat Zulkarnyaev
 */
public interface DynamicPropertyMarshaller {

    String marshall(Object marshalledObject) throws DynamicPropertySerializationException;

    <T> T unmarshall(String rawString, Class<T> clazz) throws DynamicPropertySerializationException;

}
