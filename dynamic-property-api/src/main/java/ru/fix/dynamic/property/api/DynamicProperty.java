package ru.fix.dynamic.property.api;

import javax.annotation.Nonnull;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Holds property value and notifies listeners when value changes.<br>
 * Implementation should provide thread safe access to property value via {@link #get()}.<br>
 * Be aware that it is allowed for implementation not to implement listeners functionality. <br>
 * In case of {@link #delegated(Supplier)}, returned {@link DynamicProperty} will allow subscriptions but actually
 * does not provide listeners functionality and does not invoke client listeners. <br>
 * In case of {@link #of(Object)}, returned {@link DynamicProperty} does not changes
 * and does not notifies client listeners.
 * <br>
 * Use {@link PropertySubscription#setAndCallListener(PropertyListener)}  to reuse same code block
 * during first initialization and consequence updates: <br>
 * <pre>{@code
 * final PropertySubscription<MyConfig> myConfig;
 * MyService(DynamicProperty<MyConfig> property){
 *     this.myConfig = property.createSubscription()
 *                             .setAndCallListener{ oldValue, newValue ->
 *          // initialisation or reconfiguration logic
 *          // will be invoked first time with current value of the property
 *          // and then each time on property change
 *         initializeOrUpdateMyService(newValue)
 *     }
 *     void initializeOrUpdateMyService(MyConfig config){
 *      ...
 *     }
 * }
 * }</pre>
 * {@link PropertySubscription} instance returned by {@link #createSubscription()} should be kept strongly reachable. <br/>
 * As soon as {@link PropertySubscription} instance garbage collected, subscription canceled
 * and {@link PropertyListener} stop being invoked. <br/>
 * You can cancel subscription via {@link PropertySubscription#close()} <br/>
 * <br/>
 * Use {@link #get()} for thread safe access to current property value.
 * <pre>{@code
 * val messageTemplate: DynamicProperty<String>
 * fun doWork(){
 *     ...
 *     val message = buildMessage(messageTemplate.get())
 *     ...
 * }
 * }</pre>
 * Use {@link PropertySubscription#get()} if you need both: subscription for updates
 * and ability to access current value of the property
 * <pre>{@code
 * final PropertySubscription<MyConfig> myConfigSubscription;
 * MyService(DynamicProperty<MyConfig> property){
 *     this.myConfigSubscription = property.createSubscription()
 *                             .setAndCallListener{ oldValue, newValue ->
 *          // initialisation or reconfiguration logic
 *          // will be invoked first time with current value of the property
 *          // and then each time on property change
 *         initializeOrUpdateMyService(newValue)
 *     }
 *     void initializeOrUpdateMyService(MyConfig config){
 *      ...
 *     }
 *     void doWork(){
 *          ...
 *          // access to current value of the property through it's subscription
 *          val currentConfig = myConfigSubscription.get()
 *          ...
 *     }
 * }
 * }</pre>
 * <br>
 * Different implementations provides different guarantees
 * in terms of atomicity for subscription and listener invocation.<br>
 * <p>
 * Be aware of awkward behaviour due to a concurrent nature of dynamic property.<br>
 * Check for {@link DynamicProperty} implementation documentation
 * to clarify how and in which thread implementation invokes listeners.<br>
 * <br>
 * <b>Be aware:</b><br>
 * Bad example of service initialization with concurrent modification of state
 * <pre>
 * {@code
 * // DO NOT DO THAT
 * String nonVolatileNonAtomicField = ""
 * PropertySubscription subscription;
 * MyService(DynamicProperty<String> property){
 *     subscription = property.createSubscription()
 *             .subscribeAndCall{ oldValue, newValue ->
 *          // listener will be invoked in Listener thread
 *          // non thread safe field should be replaced by volatile or atomic field.
 *          nonVolatileNonAtomicField = newValue
 *     }
 * }
 * }</pre>
 * Bad example of subscription when subscription canceled by garbage collector.
 * <pre>
 * {@code
 * // DO NOT DO THAT
 * MyService(DynamicProperty<MyConfig> property){
 *     // PropertySubscription instance created, but we do not store reference on it.
 *     // PropertySubscription instance garbage collected immediately and subscription canceled.
 *     // Listener will be invoked only once during initialization, and will stop receive new updates immediately.
 *     // Listener will not receive any more notifications.
 *     property.createSubscription()
 *             .subscribeAndCall{ oldValue, newValue ->
 *                  doImportantReconfiguration(newValue)
 *     }
 * }
 * }</pre>
 */
public interface DynamicProperty<T> extends AutoCloseable {

    /**
     * @return current value of the property
     */
    T get();

    /**
     * Creates {@link PropertySubscription} instance that allows to subscribe a listener for dynamic property updates.
     * Listener will continue to receive events as long as {@link PropertySubscription} instance
     * stays strongly reachable.
     * <pre>{@code
     * final PropertySubscription mySize;
     * MyService(DynamicProperty<String> mySize){
     *     this.mySize = mySize.createSubscription().setAndCallListener{ oldSize, newSize ->
     *          // initialisation or reconfiguration logic
     *          // will be invoked first time with current value of the property
     *          // and then each time on property change
     *         initializeOrUpdateMyService(newSize)
     *     }
     * }
     * void doWork(){
     *      var currentSize = this.mySize.get()
     *      ...
     * }
     * }</pre>
     *
     * @return Subscription instance that keeps listener active.
     * As soon as {@link PropertySubscription} instance garbage collected, the listener stops receiving events. <br>
     * You can unsubscribe listener from events via {@link PropertySubscription#close()} <br>
     * {@link PropertySubscription} gives an access to current {@link DynamicProperty} value via
     * {@link PropertySubscription#get()}.
     *
     * @see DynamicProperty
     */
    @Nonnull
    PropertySubscription<T> createSubscription();

    /**
     * @return constant DynamicProperty that holds given value and never changes.
     */
    static <T> DynamicProperty<T> of(T value) {
        return new ConstantProperty<>(value);
    }

    /**
     * Return {@link DynamicProperty} proxy that delegates {@link DynamicProperty#get()}  to given supplier. <br>
     * <br>
     * Be aware that DynamicProperty created this way
     * does not notify listeners {@link PropertyListener} <br>
     * Here is an example of inappropriate usage of delegated property: <br>
     * <b>Good example:</b><br>
     * <pre>{@code
     * class Foo(supplier: Supplier<String>){
     *     // will work correctly
     *     val baz = Baz(DynamicProperty.delegated(supplier))
     * }
     * class Baz(property: DynamicProperty<String>){
     *     fun doWork(){
     *          if(property.get().contains(...))
     *     }
     * }
     * }</pre>
     * <b>Bad example:</b><br>
     * <pre>{@code
     * class Foo(supplier: Supplier<String>){
     *     // will not work correctly since bar is actually needs listener support
     *     val bar = Bar(DynamicProperty.delegated(supplier))
     * }
     *
     * class Bar(property: DynamicProperty<String>){
     *     subscription = property.createSubscription()
     *             .setListenerAndCall{value -> ...}
     * }
     * }</pre>
     * If you need a DynamicProperty with full listeners support backed up by {@link Supplier}
     * use DynamicPropertyPoller instead
     *
     * @return DynamicProperty backed up with {@link Supplier}.
     * @see ru.fix.dynamic.property.polling.DynamicPropertyPoller
     */
    static <T> DynamicProperty<T> delegated(Supplier<T> supplier) {
        return new DelegatedProperty<>(supplier);
    }

    /**
     * Builds one property based on another.
     * <pre>{@code
     *  DynamicProperty<String> stringProperty = ...;
     *  val service = new ServiceThatRequiresIntProperty( stringProperty.map { str -> str.toInt() } )
     * }</pre>
     * <pre>{@code
     *  DynamicProperty<BigConfig> bigConfigProperty = ...:
     *  val service = new ServiceThatRequiresTimeoutInMilliseconds(
     *          bigConfigProperty.map { config -> config.timeoutInSeconds * 1000 } )
     * }</pre>
     */
    default <R> DynamicProperty<R> map(Function<T, R> map) {
        return new MappedProperty<>(this, map);
    }

    /**
     * Close this instance of {@link DynamicProperty}.
     * Unsubscribe all {@link PropertySubscription} with {@link PropertyListener}s from this instance.
     * This {@link DynamicProperty} instance will stay strongly reachable
     * from all {@link PropertySubscription} instances its created
     */
    void close();
}
