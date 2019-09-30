package ru.fix.dynamic.property.api;


@FunctionalInterface
public interface DynamicPropertyListener<T> {

    /**
     *  Method called if the value of DynamicProperty changes
     *  For some types of {@link DynamicProperty} and {@link ru.fix.dynamic.property.api.source.DynamicPropertySource}
     *  listener notification can be optional.
     *  E.g. {@link ConstantProperty} will not rise notification about property change.
     *
     *  During invocation of the listener current value of the {@link DynamicProperty#get()} is not older that newValue.
     *  Due to a concurrent nature of the {@link DynamicProperty} newValue can be already stale and actual value of
     *  the property received though {@link DynamicProperty#get()} can differ from the on received through newValue
     *  argument of notification.
     *
     *  Be aware that is is possible to be invoked with the same oldValue and newValue. Implement resource releasing and
     *  acquiring logic accordingly.
     *
     *  Be aware that method {@link DynamicProperty#addAndCallListener(DynamicPropertyListener)} invokes listener
     *  with null oldValue.
     *
     * @param oldValue previous value of the property or null if not existed before
     * @param newValue new value of the property
     */
    void onPropertyChanged(T oldValue, T newValue);
}
