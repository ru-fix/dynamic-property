package ru.fix.dynamic.property.api;

public interface DynamicProperty<T> {

    T get();

    void addListener(DynamicPropertyListener<T> listener);

    static <T> DynamicProperty<T> of(T value) {
        return new ConstantProperty<>(value);
    }
}
