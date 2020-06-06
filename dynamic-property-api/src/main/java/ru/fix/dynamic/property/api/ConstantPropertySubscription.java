package ru.fix.dynamic.property.api;

import javax.annotation.Nonnull;

/**
 * Subscription that returns constant value and does not invoke listener.
 * Used for convenient configuration of injected subscriptions
 * <pre>{@code
 * class MyService{
 *   @PropertyId("my.timeout")
 *   private PropertySubscription<MySetting> timeout = PropertySubscription.of(100)
 * }
 * }</pre>
 * @param <T>
 */
public class ConstantPropertySubscription<T> implements PropertySubscription<T> {
    private final T value;

    public ConstantPropertySubscription(T value) {
        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public PropertySubscription<T> setAndCallListener(@Nonnull PropertyListener<T> listener) {
        listener.onPropertyChanged(null, value);
        return this;
    }

    @Override
    public void close() {
    }
}
