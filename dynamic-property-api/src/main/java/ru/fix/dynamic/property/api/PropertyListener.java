package ru.fix.dynamic.property.api;

@FunctionalInterface
public interface PropertyListener<T> {
    /**
     * Method called if the value of DynamicProperty changes
     * For some implementation of {@link DynamicProperty} and {@link ru.fix.dynamic.property.api.source.DynamicPropertySource}
     * listener notification can be optional.
     * E.g. {@link ConstantProperty} will not rise notification about property change.
     * {@link DelegatedProperty} will not rise notification when supplier start return different value.
     * <p>
     * <pre>{@code
     * mySize.createSubscription()
     *       .setAndCallListener((oldValue, newValue)->{
     *          //directValue can be different from newValue
     *          //there could be other value update notifications waiting in queue
     *          directValue = mySize.get()
     *       });
     *
     * }</pre>
     * Due to a concurrent nature of the {@link DynamicProperty} newValue received by listener can be already stale and actual value of
     * the property accessed though {@link DynamicProperty#get()} can differ from the one received through newValue
     * argument of the listener.
     * <p>
     * Be aware that it is possible for listener to be invoked with the same oldValue and newValue.
     * <p>
     * Method {@link PropertySubscription#setAndCallListener(PropertyListener)} invokes listener
     * with null oldValue.
     *
     * @param oldValue previous value of the property or null if not existed before
     *                 or this is first invocation that happened during listener attachment
     *                 by {@link PropertySubscription#setAndCallListener(PropertyListener)}
     * @param newValue new value of the property
     */
    void onPropertyChanged(T oldValue, T newValue);
}
