package ru.fix.dynamic.property.api;

import ru.fix.dynamic.property.api.source.DynamicPropertySource;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class SubscriptionTracker {

    private static final Map<Object, Set<PropertySubscription<?>>> activeSubscriptions =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static void registerSubscription(Object subscriber, PropertySubscription<?> subscription) {
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

    public static void removeSubscription(Object subscriber, PropertySubscription<?> subscription) {
        activeSubscriptions.computeIfPresent(subscriber, (key, set) -> {
            set.remove(subscription);
            if (set.isEmpty()) {
                return null;
            } else {
                return set;
            }
        });
    }
}
