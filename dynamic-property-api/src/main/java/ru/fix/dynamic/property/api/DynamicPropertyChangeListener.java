package ru.fix.dynamic.property.api;


@FunctionalInterface
public interface DynamicPropertyChangeListener<T> {

    /**
     * @param value new value
     */
    void onPropertyChanged(T value);
}
