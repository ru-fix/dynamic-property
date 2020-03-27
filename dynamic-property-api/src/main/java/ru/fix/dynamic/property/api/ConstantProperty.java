package ru.fix.dynamic.property.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ConstantProperty<T> implements DynamicProperty<T> {
    private final T value;

    public ConstantProperty(T value) {
        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }

    @Nonnull
    @Override
    public PropertySubscription<T> subscribeAndCall(@Nullable Object subscriber, @Nonnull PropertyListener<T> listener) {
        listener.onPropertyChanged(null, value);

        return new PropertySubscription<T>() {
            @Override
            public T get() {
                return value;
            }

            @Override
            public void close() {
            }
        };
    }

    @Override
    public void close() {
    }
}
