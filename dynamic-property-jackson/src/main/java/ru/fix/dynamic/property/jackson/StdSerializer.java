package ru.fix.dynamic.property.jackson;

import ru.fix.dynamic.property.jackson.MarshallerBuilder.InternalMarshaller;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Provides serialization capabilities for standard JVM types
 */
public class StdSerializer implements InternalMarshaller {
    private static final HashMap<Class, Function<Object, String>> exactTypeMarshallers = new HashMap<>();
    private static final HashMap<Class, Function<String, Object>> exactTypeUnmarshallers = new HashMap<>();

    private static final LinkedHashMap<Predicate<Class>, Function<Object, String>> conditionalMarshallers = new LinkedHashMap<>();
    private static final HashMap<Predicate<Class>, Function<String, Object>> conditionalUnmarshallers = new HashMap<>();


    static {
        exactTypeMarshallers.put(Boolean.class, value -> value.toString());
        exactTypeMarshallers.put(Byte.class, value -> value.toString());
        exactTypeMarshallers.put(Short.class, value -> value.toString());
        exactTypeMarshallers.put(Integer.class, value -> value.toString());
        exactTypeMarshallers.put(Long.class, value -> value.toString());
        exactTypeMarshallers.put(Float.class, value -> value.toString());
        exactTypeMarshallers.put(Double.class, value -> value.toString());
        exactTypeMarshallers.put(BigDecimal.class, value -> value.toString());
        exactTypeMarshallers.put(BigInteger.class, value -> value.toString());
        exactTypeMarshallers.put(Duration.class, value -> value.toString());
        exactTypeMarshallers.put(String.class, value -> value.toString());

        exactTypeUnmarshallers.put(Boolean.class, string -> Boolean.valueOf(string));
        exactTypeUnmarshallers.put(Byte.class, string -> Byte.valueOf(string));
        exactTypeUnmarshallers.put(Short.class, string -> Short.valueOf(string));
        exactTypeUnmarshallers.put(Integer.class, string -> Integer.valueOf(string));
        exactTypeUnmarshallers.put(Long.class, string -> Long.valueOf(string));
        exactTypeUnmarshallers.put(Float.class, string -> Float.valueOf(string));
        exactTypeUnmarshallers.put(Double.class, string -> Double.valueOf(string));
        exactTypeUnmarshallers.put(BigDecimal.class, string -> new BigDecimal(string));
        exactTypeUnmarshallers.put(BigInteger.class, string -> new BigInteger(string));
        exactTypeUnmarshallers.put(Duration.class, string -> Duration.parse(string));
        exactTypeUnmarshallers.put(String.class, string -> string);

        conditionalMarshallers.put(type -> Path.class.isAssignableFrom(type), value -> value.toString());

        conditionalUnmarshallers.put(type -> Path.class.isAssignableFrom(type), string -> Paths.get(string));
    }

    /**
     * @return empty if no suitable serializer found for given type
     */
    @Override
    public Optional<Object> unmarshall(String rawString, Class type) {
        Function<String, Object> exact = exactTypeUnmarshallers.get(type);
        if (exact != null) {
            return Optional.of(exact.apply(rawString));
        }

        for (Map.Entry<Predicate<Class>, Function<String, Object>> condUnmarshaller : conditionalUnmarshallers.entrySet()) {
            Predicate<Class> condition = condUnmarshaller.getKey();
            Function<String, Object> marshaller = condUnmarshaller.getValue();
            if (condition.test(type)) {
                return Optional.of(marshaller.apply(rawString));
            }
        }

        return Optional.empty();
    }


    /**
     * @return empty if no suitable serializer found for given type
     */
    @Override
    public Optional<String> marshall(Object marshalledObject) {
        Objects.requireNonNull(marshalledObject);

        Function<Object, String> std = exactTypeMarshallers.get(marshalledObject.getClass());
        if (std != null) {
            return Optional.of(std.apply(marshalledObject));
        }

        for (Map.Entry<Predicate<Class>, Function<Object, String>> condMarshaller : conditionalMarshallers.entrySet()) {
            if (condMarshaller.getKey().test(marshalledObject.getClass())) {
                return Optional.of(condMarshaller.getValue().apply(marshalledObject));
            }
        }

        return Optional.empty();
    }
}
