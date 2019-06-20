package ru.fix.dynamic.property.api;

public class ConstantProperty<T> implements DynamicProperty<T> {
    final T value;

    public ConstantProperty(T value) {
        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public void addListener(DynamicPropertyChangeListener<T> listener) {
    }
}
