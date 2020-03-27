package ru.fix.dynamic.property.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CombinedProperty<R> implements DynamicProperty<R> {

    private final AtomicProperty<R> property;
    private final List<PropertySubscription<?>> subscriptions;

    public CombinedProperty(Collection<DynamicProperty<?>> sources, Supplier<R> combiner) {
        property = new AtomicProperty<>();
        subscriptions = sources.stream()
                .map(source -> source.subscribeAndCall(null,
                        (anyOldValue, anyNewValue) -> property.set(combiner.get())))
                .collect(Collectors.toList());
    }

    @Override
    public R get() {
        return property.get();
    }

    @Nonnull
    @Override
    public PropertySubscription<R> subscribeAndCall(@Nullable Object subscriber, @Nonnull PropertyListener<R> listener) {
        return property.subscribeAndCall(subscriber, listener);
    }

    @Override
    public void close() {
        property.close();
    }
}
