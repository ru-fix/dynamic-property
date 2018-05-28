package ru.fix.dynamic.property.api;

public interface DynamicProperty<T> {

    T get();

    void addListener(DynamicPropertyListener<T> listener);
}
