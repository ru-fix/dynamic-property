package ru.fix.dynamic.property.api;

import java.util.Collection;
import java.util.function.Supplier;

public class CombinedProperty<R> implements DynamicProperty<R> {

    private final AtomicProperty<R> property;

    public CombinedProperty(Collection<DynamicProperty<?>> sources, Supplier<R> combiner) {
        property = new AtomicProperty<>(null);
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
    public DynamicProperty<R> addAndCallListener(DynamicPropertyListener<R> listener) {
        return property.addAndCallListener(listener);
    }

    @Override
    public R addListenerAndGet(DynamicPropertyListener<R> listener) {
        return property.addListenerAndGet(listener);
    }

    @Override
    public DynamicProperty<R> removeListener(DynamicPropertyListener<R> listener) {
        return property.removeListener(listener);
    }

    @Override
    public DynamicProperty<R> addListener(DynamicPropertyListener<R> listener) {
        return property.addListener(listener);
    }

    @Override
    public void close() throws Exception {
        property.close();
    }
}
