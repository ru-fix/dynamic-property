package ru.fix.dynamic.property.jackson;

import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller;
import ru.fix.dynamic.property.jackson.serializer.composable.ComposableSerializer;
import ru.fix.dynamic.property.jackson.serializer.composable.JacksonSerializer;
import ru.fix.dynamic.property.jackson.serializer.composable.StdSerializer;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Builder of {@link ComposableDynamicPropertyMarshaller} that allows to add custom serializers/
 * Builder add {@link StdSerializer} as the first serializer and {@link JacksonSerializer} as the last serializer.
 */
public class MarshallerBuilder {

    private final Set<ComposableSerializer> serializers = new LinkedHashSet<>();

    private MarshallerBuilder() {
        serializers.add(new StdSerializer());
    }

    public static MarshallerBuilder newBuilder() {
        return new MarshallerBuilder();
    }

    /**
     * @return marshaller without custom serializers
     */
    public static DynamicPropertyMarshaller createDefault() {
        return newBuilder().build();
    }

    public MarshallerBuilder addSerializer(ComposableSerializer marshaller) {
        serializers.add(marshaller);
        return this;
    }

    public MarshallerBuilder addSerializers(Collection<ComposableSerializer> marshallerList) {
        serializers.addAll(marshallerList);
        return this;
    }

    public DynamicPropertyMarshaller build() {
        serializers.add(new JacksonSerializer());
        return new ComposableDynamicPropertyMarshaller(serializers);
    }

}
