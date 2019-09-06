package ru.fix.dynamic.property.api;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Holds property value and notify listeners when value is changed.
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
     * @param listener Listener is activated whenever property value changes.
     *                 It is implementation specific in what thread listener will be invoked.
     */
    DynamicProperty<T> addListener(DynamicPropertyListener<T> listener);

    /**
     * Add listener to dynamic property and invoke listener after that.
     * Be aware of awkward behaviour due to a concurrent nature of dynamic property.
     * It is possible that this method could lead to two invocation of the listener.
     * Suppose that current value of the property is 1.
     * Dynamic property implementation is being informed by external storage that property changed from 1 to 2.
     * In same time user invokes {@link #addAndCallListener(DynamicPropertyListener)}.
     * It is possible that as first listeners will be invoed
     *
     *
     * @param listener
     * @return
     */
    T addAndCallListener(DynamicPropertyListener<T> listener);

    static <T> DynamicProperty<T> of(T value) {
        return new ConstantProperty<>(value);
    }

    static <T> DynamicProperty<T> of(Supplier<T> supplier) {
        return new ConstantProperty<>(supplier.get());
    }

    default <R> DynamicProperty<R> map(Function<T, R> map) {
        return new MappedProperty<>(this, map);
    }
}
