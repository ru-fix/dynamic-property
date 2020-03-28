package ru.fix.dynamic.property.api;

import javax.annotation.Nonnull;

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
    public PropertySubscription<T> createSubscription(){
        return new PropertySubscription<T>() {
            @Override
            public PropertySubscription<T> setAndCallListener(@Nonnull PropertyListener<T> listener) {
                listener.onPropertyChanged(null, value);
                return this;
            }

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
