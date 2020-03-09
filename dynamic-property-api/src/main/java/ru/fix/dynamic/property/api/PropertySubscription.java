package ru.fix.dynamic.property.api;

/**
 * <pre>{@code
 * final PropertySubscription<Int> poolSize;
 * MyService(DynamicProperty<Int> poolSize){
 *  //replace DynamicProperty reference with PropertySubscription
 *  //PropertySubscription field will keep subscription strongly reachable and listener continue to receive events
 *  this.poolSize = poolSize.callAndSubscribe(this, (oldValue, newValue)-> setPoolSize(newValue));
 * }
 * void doWork(){
 *  var currentPoolSize = this.poolSize.get()
 *  ...
 * }
 * }</pre>
 *
 * {@link DynamicProperty#subscribeAndCall(Object, PropertyListener)} creates a {@link PropertySubscription} instance.
 * {@link PropertyListener} will continue to receive notifications
 * as long as {@link PropertySubscription} instance stays strongly reachable.
 * {@link PropertySubscription} can be canceled via {@link PropertySubscription#close()}.
 * {@link PropertySubscription} keeps strongly reachable both {@link DynamicProperty} and {@link PropertyListener}
 * There is no need in manually keeping {@link PropertySubscription} alive,
 * since it is linked with subscriber instance via {@link java.util.WeakHashMap} and stays alive
 * as long as subscriber instance stays strongly reachable.
 */
public interface PropertySubscription<T> extends AutoCloseable {
    /**
     * @return current value of the property to which this Subscription is subscribed.
     */
    T get();

    /**
     * Listener linked with current subscription stops receiving events.
     * DynamicProperty and PropertyListener stays strongly reachable from this subscription instance after close method.
     */
    @Override
    void close();
}
