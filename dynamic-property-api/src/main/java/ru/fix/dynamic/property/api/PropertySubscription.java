package ru.fix.dynamic.property.api;

import javax.annotation.Nonnull;

/**
 * <pre>{@code
 * final PropertySubscription<Int> poolSize;
 * MyService(DynamicProperty<Int> poolSize){
 *  //replace DynamicProperty reference with PropertySubscription
 *  //PropertySubscription field will keep subscription instance strongly reachable
 *  // and listener continue to receive events
 *  this.poolSize = poolSize.createSubscription()
 *                          .setAndCallListener((oldValue, newValue)-> makeAdjustmentsBasedOnNewPoolSize(newValue));
 * }
 * void doWork(){
 *  var currentPoolSize = poolSize.get()
 *  ...
 * }
 * }</pre>
 * <p>
 * {@link DynamicProperty#createSubscription()} creates a {@link PropertySubscription} instance. <br>
 * {@link PropertySubscription#setAndCallListener(PropertyListener)} attach listener to the subscription. <br>
 * {@link PropertyListener} will continue to receive notifications
 * as long as {@link PropertySubscription} instance stays strongly reachable.<br>
 * {@link PropertySubscription} can be canceled via {@link PropertySubscription#close()}. <br>
 * {@link PropertySubscription} keeps strongly reachable both {@link DynamicProperty} instance and {@link PropertyListener} <br>
 * It is important to keep {@link PropertySubscription} instance strongly reachable
 * in order for {@link PropertyListener} to continue receive events.
 */
public interface PropertySubscription<T> extends AutoCloseable {
    /**
     * @return current value of the source {@link DynamicProperty} that created this {@link PropertySubscription} instance.
     */
    T get();

    /**
     * Attaches listener to the current subscription. <br>
     * Subscribes listener for dynamic property updates and invokes this listener with current value of the property.<br>
     * During first invocation of the listener {@link PropertyListener#onPropertyChanged(Object, Object)}
     * oldValue is going to be null.
     * It is implementation specific in which thread the listener will be invoked after subscription. <br>
     * Be aware that usage of this method could lead to awkward behaviour in term of concurrency. <br>
     * See {@link DynamicProperty} interface documentation for details.
     * <pre>{@code
     * final PropertySubscription<Int> mySize;
     * MyService(DynamicProperty<Int> mySize){
     *     this.mySize = mySize.createSubscription()
     *                         .setAndCallListener{ oldSize, newSize ->
     *                           // initialisation or reconfiguration logic
     *                           // will be invoked first time with current value of the property
     *                           // and then each time on property change
     *                           initializeOrUpdateMyService(newSize)
     *                          }
     * }
     * void doWork(){
     *      var currentSize = this.mySize.get()
     *      ...
     * }
     * void initializeOrUpdateMyService(Int size){
     *  ...
     * }
     * }</pre>
     *
     * @param listener receives property change events.
     *                 Listener is activated first time with current value of the property
     *                 and then each time when property changes.
     * @return current subscription
     */
    PropertySubscription<T> setAndCallListener(@Nonnull PropertyListener<T> listener);

    /**
     * Cancel subscription.
     * Listener linked with current subscription stops receiving events.
     * After closing subscription, {@link DynamicProperty} and {@link PropertyListener} stays strongly reachable
     * from this subscription instance.
     * Second invocation of close method has no effect.
     */
    @Override
    void close();
}
