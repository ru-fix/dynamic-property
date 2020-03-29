package ru.fix.dynamic.property.api.source;

import ru.fix.dynamic.property.api.DynamicProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Storage that stores {@link DynamicProperty} values and notify {@link DynamicProperty} instances
 * when property values changes.
 */
public interface DynamicPropertySource extends AutoCloseable {

    /**
     * Subscription of {@link DynamicProperty} instance to {@link DynamicPropertySource} events. <br>
     * When {@link Subscription} instance became weakly reachable <br>
     * corresponding {@link Listener} stops receiving events. <br>
     * Same happens if {@link Subscription} closed explicitly via {@link AutoCloseable#close()} <br>
     * <br>
     */
    interface Subscription<T> extends AutoCloseable {

        /**
         * Subscribes listener for property source events and immediately invokes listener with current property value.
         * Listener will receive current or updated value from the source or defaultValue
         * if source does not have defined property.
         * Listener will be triggered on add/update/remove events for specified property.
         *
         * @param listener listener that will be triggered first time during subscription and then each time when property
         *                 changes it's value
         * @return this instance of {@link Subscription}
         */
        Subscription setAndCallListener(@Nonnull Listener<T> listener);

        /**
         * Stop subscription.
         * Corresponding {@link Listener} stops receiving events.
         */
        void close();
    }

    @FunctionalInterface
    interface Listener<T> {
        void onPropertyChanged(@Nullable T newValue);
    }

    /**
     * Create subscription for a property.
     * Use {@link Subscription#setAndCallListener(Listener)} to start listen for events.
     *
     * @param propertyName property name to identify property within PropertySource
     * @param propertyType Class of the property
     * @param defaultValue Value that will be used if property does not exist in the source.
     * @return subscription instance that control how long listener will continue to receive events.
     * If subscription instance became weakly reachable or subscription will be closed explicitly via {@link Subscription#close()}
     * the listener will stop receiving events.
     * This approach decouple {@link DynamicPropertySource} from listeners and allows GC to gather {@link DynamicProperty} instances
     * that no longer needed.
     */
    @Nonnull
    <T> Subscription<T> createSubscription(
            @Nonnull String propertyName,
            @Nonnull Class<T> propertyType,
            @Nonnull OptionalDefaultValue<T> defaultValue);

}
