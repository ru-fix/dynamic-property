package ru.fix.dynamic.property.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides simple implementation for DynamicProperty.
 * Allows manually control DynamicProperty behavior for tests.
 *
 * @author Kamil Asfandiyarov
 */
public class AtomicProperty<T> implements DynamicProperty<T> {

    private final AtomicReference<T> holder = new AtomicReference<>();
    private final List<DynamicPropertyListener<T>> listeners = new CopyOnWriteArrayList<>();

    public AtomicProperty(T value) {
        this.holder.set(value);
    }

    public void set(T value) {
        holder.set(value);
        listeners.forEach(listener -> listener.onPropertyChanged(value));
    }

    @Override
    public T get() {
        return holder.get();
    }

    @Override
    public void addListener(DynamicPropertyListener<T> listener) {
        listeners.add(listener);
    }
}
