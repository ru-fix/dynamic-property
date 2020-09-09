package ru.fix.dynamic.property.jackson;

import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class MarshallerBuilder {

    private final List<InternalMarshaller> marshallers = new LinkedList<>();

    private MarshallerBuilder() {
        marshallers.add(new StdSerializer());
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

        /**
         * Marshall object to string value
         *
         * @return empty if the current marshaller is not suitable for given type
         */
        Optional<String> marshall(Object marshalledObject);

        /**
         * Unmarshall object from string value
         *
         * @return empty if the current marshaller is not suitable for given type
         */
        <T> Optional<T> unmarshall(String rawString, Class<T> clazz);
    }
}
