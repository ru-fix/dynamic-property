package ru.fix.dynamic.property.api;

@FunctionalInterface
public interface PropertyListener<T> {
    /**
     * Method called if the value of DynamicProperty changes
     * For some implementation of {@link DynamicProperty} and {@link ru.fix.dynamic.property.api.source.DynamicPropertySource}
     * listener notification can be optional.
     * E.g. {@link ConstantProperty} will not rise notification about property change.
     * <p>
     * Due to a concurrent nature of the {@link DynamicProperty} newValue can be already stale and actual value of
     * the property received though {@link DynamicProperty#get()} can differ from the one received through newValue
     * argument of the notification.
     * <p>
     * Be aware that it is possible for listener to be invoked with the same oldValue and newValue.
     * <p>
     * Method {@link DynamicProperty#callAndSubscribe(Object, PropertyListener)}} invokes listener
     * with null oldValue.
     *
     * @param oldValue previous value of the property or null if not existed before
     * @param newValue new value of the property
     */
    void onPropertyChanged(T oldValue, T newValue);
}
