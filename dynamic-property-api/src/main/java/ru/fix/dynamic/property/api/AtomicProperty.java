package ru.fix.dynamic.property.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.stdlib.reference.ReferenceCleaner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;
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
 * <p>
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
 * <p>
 * In order to prevent all four problems user of AtomicProperty should organize proper thread safe usages of the property.
 *
 * @author Kamil Asfandiyarov
 */
public class AtomicProperty<T> implements DynamicProperty<T> {
    private static final Logger log = LoggerFactory.getLogger(AtomicProperty.class);

    private final Object lock = new Object();
    private final AtomicReference<T> holder = new AtomicReference<>();
    private final Collection<WeakReference<PropertyListener<T>>> listeners = new ConcurrentLinkedDeque<>();
    private String name = null;

    private final ReferenceCleaner referenceCleaner = ReferenceCleaner.getInstance();

    public AtomicProperty() {
    }

    public AtomicProperty(T value) {
        this.holder.set(value);
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param newValue
     * @return old value
     */
    public T set(T newValue) {
        T oldValue;
        synchronized (lock) {
            oldValue = holder.getAndSet(newValue);
            listeners.forEach(ref -> {
                try {
                    var listener = ref.get();
                    if (listener != null) {
                        listener.onPropertyChanged(oldValue, newValue);
                    }
                } catch (Exception exc) {
                    log.error("Failed to notify listener on property change." +
                                    " Property name {}, old value {}, new value {}.",
                            name, oldValue, newValue, exc);
                }
            });
        }
        listeners.removeIf(ref -> ref.get() == null);
        return oldValue;
    }

    @Override
    public T get() {
        return holder.get();
    }

    static class Subscription<T> implements PropertySubscription<T> {
        private final AtomicProperty<T> sourceProperty;
        private final WeakReference<PropertyListener<T>> listenerWeakReference;
        private final PropertyListener<T> listener;
        private final WeakReference<Object> subscriber;

        Subscription(
                AtomicProperty<T> sourceProperty,
                WeakReference<PropertyListener<T>> listenerWeakReference,
                PropertyListener<T> listener,
                WeakReference<Object> subscriber) {
            this.sourceProperty = sourceProperty;
            this.listenerWeakReference = listenerWeakReference;
            this.listener = listener;
            this.subscriber = subscriber;
        }

        @Override
        public T get() {
            return sourceProperty.get();
        }

        @Override
        public void close() {
            sourceProperty.listeners.remove(listenerWeakReference);
            SubscriptionTracker.removeSubscription(subscriber, this);
        }
    }

    @Override
    @Nonnull
    public PropertySubscription<T> subscribeAndCall(@Nullable Object subscriber, @Nonnull PropertyListener<T> listener) {
        synchronized (lock) {
            var listenerWeakReference = new WeakReference<>(listener);
            listeners.add(listenerWeakReference);

            var subscription = new Subscription<T>(
                    this,
                    listenerWeakReference,
                    listener,
                    subscriber);

            if(subscriber != null) {
                SubscriptionTracker.registerSubscription(subscriber, subscription);
//                referenceCleaner.register(subscriber, null, ((reference, meta) ->
//                        SubscriptionTracker.removeAllSubscriptions(subscriber))
//                );
            }

            referenceCleaner.register(subscription, null, (reference, meta) ->
                    listeners.remove(listenerWeakReference)
            );

            listener.onPropertyChanged(null, holder.get());

            return subscription;
        }
    }

    @Override
    public void close() {
        listeners.clear();
    }

    @Override
    public String toString() {
        return "AtomicProperty(" + holder.get() + ")";
    }
}
