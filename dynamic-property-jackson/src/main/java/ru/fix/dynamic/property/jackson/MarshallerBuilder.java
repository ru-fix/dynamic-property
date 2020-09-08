package ru.fix.dynamic.property.jackson;

import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class MarshallerBuilder {

    private final StdSerializer stdSerializer = new StdSerializer();

    private List<InternalMarshaller> marshallers = new LinkedList<>();

    private MarshallerBuilder() {
        marshallers.add(stdSerializer);
    }

    public static MarshallerBuilder newBuilder() {
        return new MarshallerBuilder();
    }

    public MarshallerBuilder addMarshaller(InternalMarshaller marshaller) {
        marshallers.add(marshaller);
        return this;
    }

    public MarshallerBuilder addMarshallers(List<InternalMarshaller> marshallerList) {
        marshallers.addAll(marshallerList);
        return this;
    }

    public DynamicPropertyMarshaller build() {
        return new JacksonDynamicPropertyMarshaller(marshallers);
    }

    public interface InternalMarshaller {

        Optional<String> marshall(Object marshalledObject);

        <T> Optional<T> unmarshall(String rawString, Class<T> clazz);
    }
}
