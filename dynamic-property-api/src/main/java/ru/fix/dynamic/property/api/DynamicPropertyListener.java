package ru.fix.dynamic.property.api;

@FunctionalInterface
public interface DynamicPropertyListener<T> {
    void onPropertyChanged(T newValue);
}
