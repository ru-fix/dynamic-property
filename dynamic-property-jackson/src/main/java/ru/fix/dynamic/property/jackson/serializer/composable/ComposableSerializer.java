package ru.fix.dynamic.property.jackson.serializer.composable;

import java.util.Optional;

public interface ComposableSerializer {

    /**
     * Serialize object to string value
     *
     * @return empty if the current serializer is not suitable for given type
     */
    Optional<String> serialize(Object marshalledObject);

    /**
     * Deserialize object from string value
     *
     * @return empty if the current serializer is not suitable for given type
     */
    <T> Optional<T> deserialize(String rawString, Class<T> clazz);
}
