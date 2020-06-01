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
        if(subscription.attachedSubscriptionReference != null) {
            subscriptions.remove(subscription.attachedSubscriptionReference);
            subscription.attachedSubscriptionReference = null;
        }
    }

    private void attachSubscriptionAndCallListener(Subscription<T> subscription){
        detachSubscription(subscription);

        synchronized (changeValueAndAddListenerLock) {

            if(subscription.attachedSubscriptionReference != null){
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
