package ru.fix.dynamic.property.api;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Holds property value and notify listeners when value is changed.<br>
 * Implementation should provide thread safe access to property value.<br>
 * <br>
 * Example of service initialization and subscription<br>
 * <pre>
 * {@code
 * MyService(DynamicProperty<String> property){
 *     property.addAndCallListener{ value ->
 *          // initialisation or reconfiguration logic
 *          // will be invoked first time with current value of the property
 *          // and then each time on property change
 *         initializeOrUpdateMyService(value)
 *     }
 * }
 * }</pre>
 * Different implementations provides different guarantees
 * in terms of atomicity subscription and listener invocation.<br>
 *
 * Be aware of awkward behaviour due to a concurrent nature of dynamic property.<br>
 * Check for {@link DynamicProperty} implementation documentation
 * to clarify how and in which thread implementation invokes listeners.<br>
 * <br>
 * <b>Problem 1:</b><br>
 * Suppose that current value of the property is 1.<br>
 * Value of property in the {@link DynamicProperty} implementation storage was changed from 1 to 2.<br>
 * Bad example of service initialization:<br>
 * <pre>
 * {@code
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
 * <pre>
 * {@code
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
public interface DynamicProperty<T>  extends AutoCloseable{

    /**
     * @return current value of property
     */
    T get();

    /**
     * Add listener to dynamic property
     *
     * @param listener Listener is activated whenever property value changes.
     *                 It is implementation specific in what thread listener will be invoked.
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



    static <T> DynamicProperty<T> of(T value) {
        return new ConstantProperty<>(value);
    }

    static <T> DynamicProperty<T> of(Supplier<T> supplier) {
        return new ConstantProperty<>(supplier.get());
    }

    default <R> DynamicProperty<R> map(Function<T, R> map) {
        return new MappedProperty<>(this, map);
    }
}
