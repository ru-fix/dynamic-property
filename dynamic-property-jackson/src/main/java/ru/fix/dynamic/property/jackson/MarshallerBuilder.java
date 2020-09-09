package ru.fix.dynamic.property.jackson;

import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller;
import ru.fix.dynamic.property.jackson.serializer.composable.ComposableSerializer;
import ru.fix.dynamic.property.jackson.serializer.composable.JacksonSerializer;
import ru.fix.dynamic.property.jackson.serializer.composable.StdSerializer;

import java.util.LinkedList;
import java.util.List;

public class MarshallerBuilder {

    private final List<ComposableSerializer> serializers = new LinkedList<>();

    private MarshallerBuilder() {
        serializers.add(new StdSerializer());
    }

    public static MarshallerBuilder newBuilder() {
        return new MarshallerBuilder();
    }

    public MarshallerBuilder addSerializer(ComposableSerializer marshaller) {
        serializers.add(marshaller);
        return this;
    }

    public MarshallerBuilder addSerializers(List<ComposableSerializer> marshallerList) {
        serializers.addAll(marshallerList);
        return this;
    }

    public DynamicPropertyMarshaller build() {
        return new ComposableDynamicPropertyMarshaller(serializers, new JacksonSerializer());
    }
}
