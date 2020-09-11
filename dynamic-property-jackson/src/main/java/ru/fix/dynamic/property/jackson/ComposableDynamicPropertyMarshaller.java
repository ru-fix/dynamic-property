package ru.fix.dynamic.property.jackson;

import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller;
import ru.fix.dynamic.property.api.marshaller.exception.DynamicPropertySerializationException;
import ru.fix.dynamic.property.jackson.serializer.composable.ComposableSerializer;
import ru.fix.dynamic.property.jackson.serializer.composable.exception.NotFoundSerializerException;

import java.util.*;


/**
 * Implementation of {@link DynamicPropertyMarshaller} which provides serialization and deserialization by Jacskon.
 */
public class ComposableDynamicPropertyMarshaller implements DynamicPropertyMarshaller {

    private final Set<ComposableSerializer> serializers;

    ComposableDynamicPropertyMarshaller(Set<ComposableSerializer> serializers) {
        this.serializers = Collections.unmodifiableSet(serializers);
    }

    @Override
    public String marshall(Object marshalledObject) {
        Objects.requireNonNull(marshalledObject);
        try {
            for (ComposableSerializer serializer : serializers) {
                Optional<String> result = serializer.serialize(marshalledObject);
                if (result.isPresent()) {
                    return result.get();
                }
            }
        } catch (Exception e) {
            throw new DynamicPropertySerializationException(
                    "Failed to serialize. Type: " + marshalledObject.getClass() + ". Instance: " + marshalledObject, e);
        }
        throw new NotFoundSerializerException(
                "Not found serializer for object. Type: " + marshalledObject.getClass() + ". Instance: "
                        + marshalledObject);
    }

    @Override
    public <T> T unmarshall(String rawString, Class<T> type) {
        Objects.requireNonNull(rawString);
        Objects.requireNonNull(type);
        try {
            for (ComposableSerializer serializer : serializers) {
                Optional<T> result = serializer.deserialize(rawString, type);
                if (result.isPresent()) {
                    return result.get();
                }
            }
        } catch (Exception e) {
            throw new DynamicPropertySerializationException(
                    "Failed to deserialize. Type: " + type + ". From " + rawString, e);
        }
        throw new NotFoundSerializerException(
                "Not found serializer for object. Type: " + type + ". From " + rawString);
    }
}
