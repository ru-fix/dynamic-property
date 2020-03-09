package ru.fix.dynamic.property.api;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CombinedProperty<R> implements DynamicProperty<R> {

    private final AtomicProperty<R> property;
    private final List<Subscription> subscriptions;

    public CombinedProperty(Collection<DynamicProperty<?>> sources, Supplier<R> combiner) {
        property = new AtomicProperty<>(null);
        subscriptions = sources.stream()
                .map(source -> source.callAndSubscribe(
                        (anyOldValue, anyNewValue) -> property.set(combiner.get())))
                .collect(Collectors.toList());
    }

    @Override
    public R get() {
        return property.get();
    }

    @Override
    public Subscription callAndSubscribe(PropertyListener<R> listener) {
        return property.callAndSubscribe(listener);
    }

    @Override
    public void unsubscribe(PropertyListener<R> listener) {
        property.unsubscribe(listener);
    }

    @Override
    public void close() {
        property.close();
    }
}
