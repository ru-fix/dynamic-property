package ru.fix.dynamic.property.jackson.serializer.composable.exception;

public class JacksonSerializeException extends RuntimeException {

    public JacksonSerializeException(String message, Throwable cause) {
        super(message, cause);
    }
}
