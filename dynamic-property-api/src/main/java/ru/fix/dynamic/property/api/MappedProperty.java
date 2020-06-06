package ru.fix.dynamic.property.api;

import java.util.function.Function;

public class MappedProperty<T, R> extends AtomicProperty<R> implements DynamicProperty<R> {

    private final PropertySubscription<T> subscription;
    private final Function<T, R> map;

    public MappedProperty(DynamicProperty<T> source, Function<T, R> map) {
        this.map = map;
        this.subscription = source
                .createSubscription()
                .setAndCallListener((oldValue, newValue) -> super.set(map.apply(newValue)));
    }

    @Override
    public R get() {
        return map.apply(subscription.get());
    }

    @Override
    public void close() {
        subscription.close();
        super.close();
    }
}
