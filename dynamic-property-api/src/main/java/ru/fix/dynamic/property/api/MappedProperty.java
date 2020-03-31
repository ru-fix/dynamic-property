package ru.fix.dynamic.property.api;

import java.util.function.Function;

public class MappedProperty<T, R> extends AtomicProperty<R> implements DynamicProperty<R> {

    private final DynamicProperty<T> dynamicProperty;
    private final DynamicPropertyListener<T> listener;

    public MappedProperty(DynamicProperty<T> dynamicProperty, Function<T, R> map) {
        this.listener = (oldValue, newValue) -> this.set(map.apply(newValue));
        this.dynamicProperty = dynamicProperty.addAndCallListener(listener);
    }

    @Override
    public void close() {
        super.close();
        dynamicProperty.removeListener(listener);
    }
}
