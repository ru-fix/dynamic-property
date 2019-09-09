package ru.fix.dynamic.property.api;

import java.util.function.Function;

public class MappedProperty<T, R> extends AtomicProperty<R> implements DynamicProperty<R> {

    public MappedProperty(DynamicProperty<T> dynamicProperty, Function<T, R> map) {
        dynamicProperty.addAndCallListener(value -> this.set(map.apply(value)));
    }
}
