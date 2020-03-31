package ru.fix.dynamic.property.api;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CombinedProperty<R>
        extends AtomicProperty<R>
        implements DynamicProperty<R> {

    private final List<PropertySubscription<?>> subscriptions;

    public CombinedProperty(Collection<DynamicProperty<?>> sources, Supplier<R> combiner) {
        subscriptions = sources
                .stream()
                .map(source -> source.createSubscription()
                        .setAndCallListener((anyOldValue, anyNewValue) -> this.set(combiner.get())))
                .collect(Collectors.toList());
    }

    @Override
    public void close() {
        subscriptions.forEach(PropertySubscription::close);
        super.close();
    }
}
