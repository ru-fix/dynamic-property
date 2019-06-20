package ru.fix.dynamic.property.api.converter;

/**
 * @author Ayrat Zulkarnyaev
 */
public class DynamicPropertySerializationException extends RuntimeException {

    public DynamicPropertySerializationException(String message) {
        super(message);
    }

    public DynamicPropertySerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DynamicPropertySerializationException(Throwable cause) {
        super(cause);
    }
}
