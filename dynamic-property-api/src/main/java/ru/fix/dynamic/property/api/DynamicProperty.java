package ru.fix.dynamic.property.api;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Holds property value and notify listener when value is changed.
 * Implementation should provide thread safe access to property value.
 */
public interface DynamicProperty<T> {

    /**
     * @return current value of property
     */
    T get();

    /**
     * Add listener to dynamic property
     *
     * @param listener Listener runs whenever property value changes.
     *                 It is implementation specific in what thread listener will be invoked.
     */
    default void addListener(DynamicPropertyListener<T> listener) {
    }

    static <T> DynamicProperty<T> of(T value) {
        return new ConstantProperty<>(value);
    }

    static <T> DynamicProperty<T> of(Supplier<T> supplier) {
        return supplier::get;
    }

    default <R> DynamicProperty<R> map(Function<T, R> map) {
        return new MappedProperty<>(this, map);
    }
}
