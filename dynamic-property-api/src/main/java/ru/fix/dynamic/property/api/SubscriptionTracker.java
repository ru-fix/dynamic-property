package ru.fix.dynamic.property.api;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class SubscriptionTracker {

    private static final Map<WeakReference<Object>, Set<PropertySubscription<?>>> activeSubscriptions =
            Collections.synchronizedMap(new WeakHashMap<>());

    //CHECK WHY WE NEED THIS TRACKER
    public static void registerSubscription(WeakReference<Object> subscriber, PropertySubscription<?> subscription) {
        activeSubscriptions.compute(
                subscriber,
                (key, set) -> {
                    if (set == null) {
                        set = Collections.newSetFromMap(new ConcurrentHashMap<>());
                    }
                    set.add(subscription);
                    return set;
                });
    }

    public static void removeSubscription(WeakReference<Object> subscriber, PropertySubscription<?> subscription) {
        activeSubscriptions.computeIfPresent(subscriber, (key, set) -> {
            set.remove(subscription);
            if (set.isEmpty()) {
                return null;
            } else {
                return set;
            }
        });
    }

    public static void removeAllSubscriptions(Object subscriber) {
        activeSubscriptions.remove(subscriber);
    }
}
