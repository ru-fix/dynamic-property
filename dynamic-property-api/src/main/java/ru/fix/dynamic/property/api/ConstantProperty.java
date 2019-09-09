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
    public ConstantProperty<T> addListener(DynamicPropertyListener<T> listener) {
        //Constant property never changes
        return this;
    }

    @Override
    public DynamicProperty<T> addAndCallListener(DynamicPropertyListener<T> listener) {
        listener.onPropertyChanged(value);
        return this;
    }

    @Override
    public T addListenerAndGet(DynamicPropertyListener<T> listener) {
        return value;
    }

    @Override
    public DynamicProperty<T> removeListener(DynamicPropertyListener<T> listener) {
        return this;
    }
}
