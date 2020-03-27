package ru.fix.dynamic.property.api.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.dynamic.property.api.AtomicProperty;
import ru.fix.dynamic.property.api.DynamicProperty;
import ru.fix.dynamic.property.api.PropertyListener;
import ru.fix.dynamic.property.api.PropertySubscription;

import javax.annotation.Nonnull;

/**
 * Contains property initial value.
 * Registers listener within {@link DynamicPropertySource}.
 * Listen for events from  {@link DynamicPropertySource}.
 * Update local value and propagates update events to it's subscribers.
 * See {@link DynamicProperty#subscribeAndCall(Object, PropertyListener)} <br>
 * <br>
 * If instance of this class became weakly reachable it will stop receiving events from {@link DynamicPropertySource}
 * Same effect will be archived through {@link DynamicProperty#close()}
 */
public class SourcedProperty<T> implements DynamicProperty<T> {
    private static final Logger log = LoggerFactory.getLogger(SourcedProperty.class);

    private final String name;
    private final Class<T> type;
    private final DynamicPropertySource propertySource;
    private final DynamicPropertySource.Subscription subscription;
    private final AtomicProperty<T> atomicProperty;

    public SourcedProperty(DynamicPropertySource propertySource,
                           String name,
                           Class<T> type,
                           OptionalDefaultValue<T> defaultValue) {
        this.name = name;
        this.type = type;
        this.propertySource = propertySource;
        this.atomicProperty = new AtomicProperty<>();
        this.atomicProperty.setName(name);

        subscription = propertySource.subscribeAndCall(
                this.name,
                this.type,
                defaultValue,
                newValue -> {
                    T oldValue = atomicProperty.set(newValue);
                    if (log.isTraceEnabled()) {
                        log.trace("Sourced property update: name: {}, type: {}, oldValue: {}, newValue: {}",
                                name,
                                type,
                                oldValue,
                                newValue);
                    }
                }
        );
    }

    @Override
    public T get() {
        return atomicProperty.get();
    }

    @Override
    @Nonnull
    public PropertySubscription<T> subscribeAndCall(@Nonnull Object subscriber,
                                                    @Nonnull PropertyListener<T> listener) {
        return atomicProperty.subscribeAndCall(subscriber, listener);
    }

    @Override
    public void close() {
        this.subscription.close();
        this.atomicProperty.close();
    }

    @Override
    public String toString() {
        return "SourcedProperty{" +
                "type=" + type +
                ", name='" + name + "'" +
                ", value=" + atomicProperty.get() +
                '}';
    }
}
