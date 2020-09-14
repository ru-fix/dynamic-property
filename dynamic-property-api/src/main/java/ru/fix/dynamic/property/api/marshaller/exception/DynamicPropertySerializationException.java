package ru.fix.dynamic.property.api.marshaller.exception;

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
}
