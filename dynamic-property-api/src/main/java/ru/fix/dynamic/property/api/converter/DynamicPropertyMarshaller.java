package ru.fix.dynamic.property.api.converter;

import ru.fix.dynamic.property.api.converter.exception.DynamicPropertyDeserializationException;
import ru.fix.dynamic.property.api.converter.exception.DynamicPropertySerializationException;

/**
 * @author Ayrat Zulkarnyaev
 */
public interface DynamicPropertyMarshaller {

    String marshall(Object marshalledObject) throws DynamicPropertySerializationException;

    <T> T unmarshall(String rawString, Class<T> clazz) throws DynamicPropertyDeserializationException;

}
