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
    public void addListener(DynamicPropertyListener<T> listener) {
        //Constant property that never changes
    }
}
