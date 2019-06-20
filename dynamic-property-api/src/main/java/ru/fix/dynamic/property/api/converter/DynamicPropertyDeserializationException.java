package ru.fix.dynamic.property.api.converter;

/**
 * @author Ayrat Zulkarnyaev
 */
public class DynamicPropertyDeserializationException extends RuntimeException {

    public DynamicPropertyDeserializationException(String message) {
        super(message);
    }

    public DynamicPropertyDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DynamicPropertyDeserializationException(Throwable cause) {
        super(cause);
    }
}
