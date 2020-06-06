package ru.fix.dynamic.property.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.stdlib.reference.CleanableWeakReference;
import ru.fix.stdlib.reference.ReferenceCleaner;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides simple implementation for DynamicProperty. <br>
 * Be aware that you can see new value update through {@link #get()}
 * before receiving update event in {@link PropertyListener}
 * Listeners will be invoked during {@link #set(Object)} in the same thread. <br>
 * <br>
 * <pre>{@code
 * MyService(DynamicProperty<Integer> property){...}
 *
 * val property = AtomicProperty(1)
 * MyService myService = MyService(property)
 * result = myService.doWork()
 * }</pre>
 * <p>
 *
 * @author Kamil Asfandiyarov
 */
public class AtomicProperty<T> implements DynamicProperty<T> {
    private static final Logger log = LoggerFactory.getLogger(AtomicProperty.class);

    private final Object changeValueAndAddListenerLock = new Object();
    private final AtomicReference<T> valueHolder = new AtomicReference<>();
    private final Set<CleanableWeakReference<Subscription<T>>> subscriptions =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private String name = null;

    private final ReferenceCleaner referenceCleaner = ReferenceCleaner.getInstance();

    public AtomicProperty() {
    }

    public AtomicProperty(T value) {
        this.valueHolder.set(value);
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
        synchronized (changeValueAndAddListenerLock) {
            oldValue = valueHolder.getAndSet(newValue);
            subscriptions.forEach(ref -> {
                try {
                    Subscription<T> subscription = ref.get();
                    if (subscription != null) {
                        subscription.listener.onPropertyChanged(oldValue, newValue);
                    }
                } catch (Exception exc) {
                    log.error("Failed to notify listener on property change." +
                                    " Property name {}, old value {}, new value {}.",
                            name, oldValue, newValue, exc);
                }
            });
        }
        subscriptions.removeIf(ref -> ref.get() == null);
        return oldValue;
    }

    @Override
    public T get() {
        return valueHolder.get();
    }

    private static class Subscription<T> implements PropertySubscription<T> {
        private final AtomicProperty<T> sourceProperty;
        private PropertyListener<T> listener;
        private CleanableWeakReference<Subscription<T>> attachedSubscriptionReference;

        Subscription(AtomicProperty<T> sourceProperty) {
            this.sourceProperty = sourceProperty;
        }

        @Override
        public T get() {
            return sourceProperty.get();
        }


        @Override
        public PropertySubscription<T> setAndCallListener(@Nonnull PropertyListener<T> listener) {
            this.listener = listener;
            this.sourceProperty.attachSubscriptionAndCallListener(this);
            return this;
        }

        @Override
        public void close() {
            sourceProperty.detachSubscription(this);
        }
    }

    @Override
    @Nonnull
    public PropertySubscription<T> createSubscription() {
        return new Subscription<>(this);
    }

    private void detachSubscription(Subscription<T> subscription) {
        if (subscription.attachedSubscriptionReference != null) {
            subscriptions.remove(subscription.attachedSubscriptionReference);
            subscription.attachedSubscriptionReference = null;
        }
    }

    private void attachSubscriptionAndCallListener(Subscription<T> subscription) {
        detachSubscription(subscription);

        synchronized (changeValueAndAddListenerLock) {

            if (subscription.attachedSubscriptionReference != null) {
                subscriptions.remove(subscription.attachedSubscriptionReference);
                subscription.attachedSubscriptionReference.cancelCleaningOrder();
                subscription.attachedSubscriptionReference = null;
            }

            CleanableWeakReference<Subscription<T>> cleanableWeakReference = referenceCleaner.register(
                    subscription,
                    null,
                    (reference, meta) -> subscriptions.remove(reference)
            );
            subscription.attachedSubscriptionReference = cleanableWeakReference;
            subscriptions.add(cleanableWeakReference);

            subscription.listener.onPropertyChanged(null, valueHolder.get());
        }
    }

    @Override
    public void close() {
        subscriptions.clear();
    }

    @Override
    public String toString() {
        return "AtomicProperty(" + valueHolder.get() + ")";
    }
}
