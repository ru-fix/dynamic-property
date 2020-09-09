package ru.fix.dynamic.property.jackson;

import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller;
import ru.fix.dynamic.property.api.marshaller.exception.DynamicPropertySerializationException;
import ru.fix.dynamic.property.jackson.serializer.composable.ComposableSerializer;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


/**
 * Implementation of {@link DynamicPropertyMarshaller} which provides serialization and deserialization by Jacskon.
 */
public class ComposableDynamicPropertyMarshaller implements DynamicPropertyMarshaller {

    private final List<ComposableSerializer> serializers;
    private final ComposableSerializer defaultSerializer;

    ComposableDynamicPropertyMarshaller(List<ComposableSerializer> serializers,
                                        ComposableSerializer defaultSerializer) {
        this.serializers = Collections.unmodifiableList(serializers);
        this.defaultSerializer = defaultSerializer;
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
            return defaultSerializer.serialize(marshalledObject).get();
        } catch (Exception e) {
            throw new DynamicPropertySerializationException(
                    "Failed to serialize. Type: " + marshalledObject.getClass() + ". Instance: " + marshalledObject, e);
        }
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
            return defaultSerializer.deserialize(rawString, type).get();
        } catch (Exception e) {
            throw new DynamicPropertySerializationException(
                    "Failed to deserialize. Type: " + type + ". From " + rawString, e);
        }
    }
}
