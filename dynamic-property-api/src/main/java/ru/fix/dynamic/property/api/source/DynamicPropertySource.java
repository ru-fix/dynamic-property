package ru.fix.dynamic.property.api.source;

import ru.fix.dynamic.property.api.DynamicProperty;
import ru.fix.dynamic.property.api.DynamicPropertyListener;

/**
 * Storage that stores {@link DynamicProperty} values and notify {@link DynamicProperty} instances when values changes.
 */
public interface DynamicPropertySource extends AutoCloseable {

    /**
     * Represent subscription of  {@link DynamicProperty} to {@link DynamicPropertySource} events. <br>
     * When {@link Subscription} instance became weakly reachable <br>
     * corresponding {@link DynamicPropertyListener} stops receiving events. <br>
     * Same happens if {@link Subscription} closed explicitly via {@link AutoCloseable} <br>
     * <br>
     * See {@link #subscribeAndCallListener(String, Class, OptionalDefaultValue, DynamicPropertyListener)} <br>
     */
    interface Subscription extends AutoCloseable {
        /**
         * Stop subscription.
         * Corresponding {@link DynamicPropertyListener} stops receiving events.
         */
        void close();
    }

    @FunctionalInterface
    interface Listener<T> {
        void onPropertyChanged(T newValue);
    }

    /**
     * Subscribe listener and immediately invokes listener with current property value.
     * Listener will receive current or updated value from the source or defaultValue if source does not have defined property.
     * Listener will be triggered on add/update/remove events for specified property.
     *
     * @param propertyName property name to identify property within PropertySource
     * @param propertyType Class of the property
     * @param defaultValue Value that will be used if property does not exist in the source.
     * @param listener     listener that will be triggered first time during subscription and then each time when property
     *                     changes it's value
     * @return subscription instance that control how long listener will continue to receive events.
     * If subscription instance became weakly reachable or subscription will be closed explicitly via {@link Subscription#close()}
     * the listener will stop receiving events.
     * This approach decouple {@link DynamicPropertySource} from listeners and allows GC to gather {@link DynamicProperty} instances
     * that no longer needed.
     */
    <T> Subscription subscribeAndCallListener(
            String propertyName,
            Class<T> propertyType,
            OptionalDefaultValue<T> defaultValue,
            Listener<T> listener);

}
