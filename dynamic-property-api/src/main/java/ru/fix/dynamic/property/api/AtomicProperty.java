package ru.fix.dynamic.property.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides simple implementation for DynamicProperty. <br>
 * Allows to manually provide DynamicProperty in tests. <br>
 * Be aware that this class does not synchronize value update and listener invocations.<br>
 * Listeners will be invoked during {@link #set(Object)} in the same thread. <br>
 * <br>
 * <pre>{@code
 * MyService(DynamicProperty<Integer> property){
 *      property.addAndCallListener(value -> initialize(value))
 * }
 * property = AtomicProperty(1)
 * MyService myService = MyService(property)
 * result = myService.doWork()
 * assertThat(result, ...)
 * }</pre>
 *
 * Be aware that listeners invoked in same thread, that calls {@link #set(Object)} <br>
 * This class does not provide any synchronization except holding volatile variable in order to stay lightweight <br>
 * Here is an example that leads to race condition:
 *
 * <pre>{@code
 * MyService(DynamicProperty<Integer> property){
 *      property.addAndCallListener(value -> initialize(value))
 * }
 * //DO NOT DO THAT
 * property = AtomicProperty(1)
 * new Thread{ property.set(2) }.start()
 * MyService myService = MyService(property)
 * //Problem 1. It is not clear what value MyService will see 1 or 2 or both.
 * //Problem 2. MyService can end up with a stale value of 1.
 * //Problem 3. MyService method initialize could be invoked twice concurrently.
 * //Problem 4. MyService method initialize could be invoked twice and see wrong order of changes:
 * // at first it will see 2 and then 1.
 * }</pre>
 *
 * In order to prevent all four problems user of AtomicProperty should organize proper thread safe usages of the property.
 * @author Kamil Asfandiyarov
 */
public class AtomicProperty<T> implements DynamicProperty<T> {
    private static final Logger log = LoggerFactory.getLogger(AtomicProperty.class);

    private final AtomicReference<T> holder = new AtomicReference<>();
    private final List<DynamicPropertyListener<T>> listeners = new CopyOnWriteArrayList<>();

    public AtomicProperty() {
    }

    public AtomicProperty(T value) {
        this.holder.set(value);
    }

    public void set(T newValue) {
        T oldValue = holder.getAndSet(newValue);
        listeners.forEach(listener -> {
            try {
                listener.onPropertyChanged(oldValue, newValue);
            } catch (Exception exc) {
                log.error("Failed to notify listener on property change. Old value{}, new value {}.",
                        oldValue, newValue, exc);
            }
        });
    }

    @Override
    public T get() {
        return holder.get();
    }

    @Override
    public DynamicProperty<T> addListener(DynamicPropertyListener<T> listener) {
        listeners.add(listener);
        return this;
    }

    @Override
    public DynamicProperty<T> addAndCallListener(DynamicPropertyListener<T> listener) {
        listeners.add(listener);
        listener.onPropertyChanged(null, holder.get());
        return this;
    }

    @Override
    public T addListenerAndGet(DynamicPropertyListener<T> listener) {
        listeners.add(listener);
        return holder.get();
    }

    @Override
    public DynamicProperty<T> removeListener(DynamicPropertyListener<T> listener) {
        listeners.remove(listener);
        return this;
    }

    @Override
    public void close() {
        listeners.clear();
    }
}
