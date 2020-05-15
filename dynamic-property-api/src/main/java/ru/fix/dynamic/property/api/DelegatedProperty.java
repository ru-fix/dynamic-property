package ru.fix.dynamic.property.api;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Be aware that DelegatedProperty
 * does not notify listeners through {@link PropertyListener}
 * All requests to {@link DynamicProperty#get()}
 * will be delegated to {@link Supplier#get()} method of the given supplier.
 * <p>
 * If you need a DynamicProperty with full listener support backed up by {@link Supplier} use DynamicPropertyPoller
 *
 * @see ru.fix.dynamic.property.polling.DynamicPropertyPoller
 */
public class DelegatedProperty<T> implements DynamicProperty<T> {

    private final Supplier<T> supplier;

    public DelegatedProperty(Supplier<T> supplier) {
        Objects.requireNonNull(supplier);
        this.supplier = supplier;
    }

    @Override
    public T get() {
        return supplier.get();
    }

    @Nonnull
    @Override
    public PropertySubscription<T> createSubscription() {
        return new PropertySubscription<T>() {
            @Override
            public PropertySubscription<T> setAndCallListener(@Nonnull PropertyListener<T> listener) {
                listener.onPropertyChanged(null, supplier.get());
                return this;
            }

            @Override
            public void close() {
            }

            @Override
            public T get() {
                return supplier.get();
            }
        };
    }

    @Override
    public void close() {
    }
}
