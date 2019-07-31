package ru.fix.dynamic.property.api;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class CombinedProperty<R> implements DynamicProperty<R> {

    private final AtomicProperty<R> property;

    public CombinedProperty(Collection<DynamicProperty<?>> sources, Supplier<R> combiner) {
        property = new AtomicProperty<>(combiner.get());
        for (DynamicProperty<?> source : sources) {
            source.addListener(any -> property.set(combiner.get()));
        }
        //This way we will not miss property update between initialization and subscription
        property.set(combiner.get());
    }

    @Override
    public R get() {
        return property.get();
    }

    @Override
    public void addListener(DynamicPropertyListener<R> listener) {
        property.addListener(listener);
    }
}
