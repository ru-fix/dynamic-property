package ru.fix.dynamic.property.api;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Be aware that SupplierProperty
 * does not notify listeners through {@link java.beans.PropertyChangeListener}
 * All requests to {@link DynamicProperty#get()}
 * will be delegated to {@link Supplier#get()} method of given supplier.
 *
 * If you need a DynamicProperty with full listener support backed up by {@link Supplier} use DynamicPropertyPoller
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

    @Override
    public DynamicProperty<T> addListener(DynamicPropertyListener<T> listener) {
        return this;
    }

    @Override
    public DynamicProperty<T> addAndCallListener(DynamicPropertyListener<T> listener) {
        listener.onPropertyChanged(null, supplier.get());
        return this;
    }

    @Override
    public T addListenerAndGet(DynamicPropertyListener<T> listener) {
        return supplier.get();
    }

    @Override
    public DynamicProperty<T> removeListener(DynamicPropertyListener<T> listener) {
        return this;
    }

    @Override
    public void close() {
    }
}
