package ru.fix.dynamic.property.jackson.serializer.composable.exception;

import ru.fix.dynamic.property.api.marshaller.exception.DynamicPropertySerializationException;

public class NotFoundSerializerException extends DynamicPropertySerializationException {

    public NotFoundSerializerException(String message) {
        super(message);
    }
}
