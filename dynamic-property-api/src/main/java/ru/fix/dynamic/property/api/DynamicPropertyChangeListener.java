package ru.fix.dynamic.property.api;

//TODO: разделить: property, property source, annotation
/**
 * WARNING
 * Listener runs in internal zookeeper thread. It should be very light, run very fast and so not use locks.
 */
@FunctionalInterface
public interface DynamicPropertyChangeListener<T> {

    /**
     * WARNING
     * Listener runs in internal zookeeper thread. It should be very light, run very fast and so not use locks.
     *
     * @param value
     */
    void onPropertyChanged(T value);
}
