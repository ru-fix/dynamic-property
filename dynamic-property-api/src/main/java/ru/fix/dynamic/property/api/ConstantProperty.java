package ru.fix.dynamic.property.api;

public class ConstantProperty<T> implements DynamicProperty<T> {
    private final T value;

    public ConstantProperty(T value) {
        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public Subscription callAndSubscribe(PropertyListener<T> listener) {
        listener.onPropertyChanged(null, value);

        return new Subscription() {
            @Override
            public void close() {
            }
        };
    }

    @Override
    public void unsubscribe(PropertyListener<T> listener) {
    }

    @Override
    public void close() {
    }
}
