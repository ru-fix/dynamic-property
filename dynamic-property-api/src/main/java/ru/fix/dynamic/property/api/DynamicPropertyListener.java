package ru.fix.dynamic.property.api;


@FunctionalInterface
public interface DynamicPropertyListener<T> {

    /**
     * @param value new value
     */
    void onPropertyChanged(T value);
}
