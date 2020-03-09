package ru.fix.dynamic.property.api;

import java.util.function.Function;

public class MappedProperty<T, R> extends AtomicProperty<R> implements DynamicProperty<R> {

    private final DynamicProperty.Subscription subscription;

    public MappedProperty(DynamicProperty<T> source, Function<T, R> map) {
        this.subscription = source.callAndSubscribe((oldValue, newValue) -> this.set(map.apply(newValue)));
    }

    @Override
    public void close() {
        subscription.close();
        super.close();
    }
}
