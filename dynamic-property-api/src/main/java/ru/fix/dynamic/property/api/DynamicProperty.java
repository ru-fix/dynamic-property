package ru.fix.dynamic.property.api;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Holds property value and notifies listeners when value is changed.<br>
 * Implementation should provide thread safe access to property value via {@link #get()}.<br>
 * Be aware that it is allowed for implementation not to implement listeners functionality. <br>
 * In case of {@link #of(Supplier)} returned {@link DynamicProperty} will allow to subscribe but actually
 * does not provide listeners functionality and does not invoke client listeners. <br>
 * <br>
 * Use {@link #addAndCallListener(DynamicPropertyListener)} to reuse same code block during first initialization
 * and consequence updates: <br>
 * <pre>{@code
 * MyService(DynamicProperty<String> property){
 *     property.addAndCallListener{ value ->
 *          // initialisation or reconfiguration logic
 *          // will be invoked first time with current value of the property
 *          // and then each time on property change
 *         initializeOrUpdateMyService(value)
 *     }
 * }
 * }</pre>
 *
 * Use {@link #get()} for thread safe access to current property value
 * <pre>{@code
 * val messageTemplate: DynamicProperty<String>
 * fun doWork(){
 *      ...
 *     val message = buildMessage(messageTemplate.get())
 *     ...
 * }
 * }</pre>
 *
 *
 * Different implementations provides different guarantees
 * in terms of atomicity subscription and listener invocation.<br>
 * <p>
 * Be aware of awkward behaviour due to a concurrent nature of dynamic property.<br>
 * Check for {@link DynamicProperty} implementation documentation
 * to clarify how and in which thread implementation invokes listeners.<br>
 * <br>
 * <b>Problem 1:</b><br>
 * Suppose that current value of the property is 1.<br>
 * Value of property in the {@link DynamicProperty} implementation storage was changed from 1 to 2.<br>
 * Bad example of service initialization:<br>
 * <pre>{@code
 * // DO NOT DO THAT
 * MyService(DynamicProperty property){
 *     val value = property.get()
 *     initialize(value)
 *     //-- we could miss property update here --
 *     property.addListener{ newValue -> initialize(newValue)}
 * }
 * }</pre>
 * It is possible that during first invocation of property.get() we will see value 1. <br>
 * Then before subscribing our listener property update will occurs and we miss update from 1 to 2. <br>
 * This lead to the situation when MyService thinks that property value is 1 but actually it is 2. <br>
 * <br>
 * <b>Problem 2:</b><br>
 * Suppose that current value of the property is 1.<br>
 * Value of property in the {@link DynamicProperty} implementation storage was changed from 1 to 2.<br>
 * Example of service initialization with concurrent execution problem
 * <pre>{@code
 * // DO NOT DO THAT
 * MyService(DynamicProperty property){
 *     property.addListener{ newValue -> initialize(newValue) }
 *     val value = property.get()
 *     //-- we could launch initialize method concurrently from user thread and listener thread --
 *     initialize(value)
 * }
 * }</pre>
 * User thread adds listener and then reads property value 1. <br>
 * User thread starts to initialize service with value 1. <br>
 * In same time DynamicProperty implementation calls concurrently listener and
 * user listener starts to initialize service with value 2. <br>
 * <br>
 * <b>Problem 3:</b><br>
 * Example of service initialization with concurrent modification of state
 * <pre>
 * {@code
 * // DO NOT DO THAT
 * String nonVolatileNonAtomicField = ""
 * MyService(DynamicProperty property){
 *     property.addListener{ newValue ->
 *          // listener invoked in Listener thread
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
     * Add listener to dynamic property. <br>
     * It is implementation specific in what thread the listener will be invoked after subscription. <br>
     * Be aware that usage of this method could lead to awkward behaviour in term of concurrency. <br>
     * See DynamicProperty interface documentation for details.
     * It is recommended to use {@link #addAndCallListener(DynamicPropertyListener)} that has more fine granted behaviour
     * in term of concurrency and order of subscription and receiving notification.
     *
     * @param listener Listener is activated whenever property value changes.
     */
    DynamicProperty<T> addListener(DynamicPropertyListener<T> listener);

    /**
     * Add listener to dynamic property and invokes it with current value of the property.
     *
     * @param listener Listener is activated first time with current value of the property and then
     *                 each time when property changes.
     * @return current instance of DynamicProperty
     */
    DynamicProperty<T> addAndCallListener(DynamicPropertyListener<T> listener);

    /**
     * Add listener to dynamic property.
     * Returns current value of the property.
     *
     * @param listener Listener is activated each time when property changes.
     * @return current value of the DynamicProperty
     */
    T addListenerAndGet(DynamicPropertyListener<T> listener);

    /**
     * Unregister listener from the property.
     */
    DynamicProperty<T> removeListener(DynamicPropertyListener<T> listener);


    /**
     * @return DynamicProperty that holds given value and never changes
     */
    static <T> DynamicProperty<T> of(T value) {
        return new ConstantProperty<>(value);
    }

    /**
     * Be aware that DynamicProperty created this way
     * does not notify listeners through {@link java.beans.PropertyChangeListener}
     * @return DynamicProperty proxy. All requests to {@link DynamicProperty#get()}
     * will be delegated to {@link Supplier#get()} method of given supplier.
     * <pre>{@code
     *
     * class Foo(supplier: Supplier<String>){
     *     // will break since bar is actually needs listener support
     *     val bar = Bar(DynamicProperty.of(supplier))
     *
     *     // will work correctly
     *     val baz = Baz(DynamicProperty.of(supplier))
     * }
     *
     * class Bar(property: DynamicProperty<String>){
     *     property.addListener{value -> ...}
     * }
     *
     * class Baz(property: DynamicProperty<String>){
     *     fun doWork(){
     *          if(property.get().contains(...))
     *     }
     * }
     *
     * }</pre>
     * If you need a DynamicProperty with full listeners support backed up by {@link Supplier}
     * use DynamicPropertyPoller instead
     * @see ru.fix.dynamic.property.polling.DynamicPropertyPoller
     */
    static <T> DynamicProperty<T> of(Supplier<T> supplier) {
        return new SuppliedProperty<>(supplier);
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
     * Unsubscribe it from {@link DynamicPropertySource}
     */
    void close();
}
