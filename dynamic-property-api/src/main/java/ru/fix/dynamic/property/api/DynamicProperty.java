package ru.fix.dynamic.property.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Holds property value and notifies listeners when value changes.<br>
 * Implementation should provide thread safe access to property value via {@link #get()}.<br>
 * Be aware that it is allowed for implementation not to implement listeners functionality. <br>
 * In case of {@link #delegated(Supplier)}, returned {@link DynamicProperty} will allow subscriptions but actually
 * does not provide listeners functionality and does not invoke client listeners. <br>
 * <br>
 * Use {@link #subscribeAndCall(Object, PropertyListener)} to reuse same code block during first initialization
 * and consequence updates: <br>
 * <pre>{@code
 * final Subscription subscription;
 * MyService(DynamicProperty<String> property){
 *     this.subscription = property.callAndSubscribe{ oldValue, newValue ->
 *          // initialisation or reconfiguration logic
 *          // will be invoked first time with current value of the property
 *          // and then each time on property change
 *         initializeOrUpdateMyService(newValue)
 *     }
 * }
 * }</pre>
 * Subscription instance returned by {@link #subscribeAndCall(Object, PropertyListener)} should be kept strongly reachable. <br/>
 * As soon as Subscription instance garbage collected, subscription canceled and {@link PropertyListener} stop being invoked. <br/>
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
 * <br>
 * Different implementations provides different guarantees
 * in terms of atomicity subscription and listener invocation.<br>
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
 * MyService(DynamicProperty property){
 *     property.subscribeAndCall{ oldValue, newValue ->
 *          // listener will be invoked in Listener thread
 *          // non thread safe field should be replaced by volatile or atomic field.
 *          nonVolatileNonAtomicField = newValue
 *     }
 * }
 * }</pre>
 */
public interface DynamicProperty<T> extends AutoCloseable {

    /**
     * @return current value of property
     */
    T get();

    /**
     * Subscribes listener for dynamic property update and invokes this listener with current value of the property.<br>
     * During first invocation of the listener {@link PropertyListener#onPropertyChanged(Object, Object)}
     *  oldValue is going to be null.
     * It is implementation specific in which thread the listener will be invoked after subscription. <br>
     * Be aware that usage of this method could lead to awkward behaviour in term of concurrency. <br>
     * See DynamicProperty interface documentation for details.
     * <pre>{@code
     * final PropertySubscription mySize;
     * MyService(DynamicProperty<String> mySize){
     *     this.mySize = mySize.callAndSubscribe{ oldSize, newSize ->
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
     * @param listener Listener is activated first time with current value of the property
     *                 and then each time when property changes.
     *
     * @return Subscription instance that keeps listener active.
     * As soon as {@link PropertySubscription} garbage collected, the listener stops receiving events. <br>
     * You can unsubscribe listener from events via {@link PropertySubscription#close()} <br>
     * {@link PropertySubscription} gives an access to current {@link DynamicProperty} value via
     * {@link PropertySubscription#get()}.
     */
    @Nonnull
    PropertySubscription<T> subscribeAndCall(@Nullable Object subscriber, @Nonnull PropertyListener<T> listener);

    /**
     * @return DynamicProperty that holds given value and never changes.
     */
    static <T> DynamicProperty<T> of(T value) {
        return new ConstantProperty<>(value);
    }

    /**
     * Return DynamicProperty proxy that delegates {@link DynamicProperty#get()}  to given supplier. <br>
     * <br>
     * Be aware that DynamicProperty created this way
     * does not notify listeners through {@link PropertyListener} <br>
     * Here is an example of inappropriate usage of delegated property: <br>
     * <b>Bad example:</b><br>
     * <pre>{@code
     * class Foo(supplier: Supplier<String>){
     *     // will not work correctly since bar is actually needs listener support
     *     val bar = Bar(DynamicProperty.delegated(supplier))
     *
     *     // will work correctly
     *     val baz = Baz(DynamicProperty.delegated(supplier))
     * }
     *
     * class Bar(property: DynamicProperty<String>){
     *     property.callAndSubscribe{value -> ...}
     * }
     *
     * class Baz(property: DynamicProperty<String>){
     *     fun doWork(){
     *          if(property.get().contains(...))
     *     }
     * }
     * }</pre>
     * If you need a DynamicProperty with full listeners support backed up by {@link Supplier}
     * use DynamicPropertyPoller instead
     * @return DynamicProperty backed up with {@link Supplier}.
     * @see ru.fix.dynamic.property.polling.DynamicPropertyPoller
     */
    static <T> DynamicProperty<T> delegated(Supplier<T> supplier) {
        return new DelegatedProperty<>(supplier);
    }

    /**
     * Builds one property based on another.
     * <pre>{@code
     *  val stringProperty: DynamicProperty<String>
     *  val service = ServiceThatRequiresIntProperty( stringProperty.map { str -> str.toInt() } )
     * }</pre>
     * <pre>{@code
     *  val bigConfigProperty: DynamicProperty<BigConfig>
     *  val service = ServiceThatRequiresTimeoutInMilliseconds( bigConfigProperty.map { config -> config.timeoutInSeconds * 1000 } )
     * }</pre>
     */
    default <R> DynamicProperty<R> map(Function<T, R> map) {
        return new MappedProperty<>(this, map);
    }

    /**
     * Close this instance of DynamicProperty.
     * Unsubscribe all {@link PropertyListener} from this instance.
     */
    void close();
}
