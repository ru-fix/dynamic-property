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
public class SourcedProperty<T>
        extends AtomicProperty<T>
        implements DynamicProperty<T> {

    private static final Logger log = LoggerFactory.getLogger(SourcedProperty.class);

    private final Class<T> type;
    private final String name;
    private final DynamicPropertySource.Subscription<T> subscription;

    public SourcedProperty(DynamicPropertySource propertySource,
                           String name,
                           Class<T> type,
                           OptionalDefaultValue<T> defaultValue) {
        this.type = type;
        this.name = name;
        super.setName(name);

        subscription = propertySource.createSubscription(name, type, defaultValue);
        subscription.setAndCallListener(newValue -> {
            T oldValue = this.set(newValue);
            if (log.isTraceEnabled()) {
                log.trace("Sourced property update: name: {}, type: {}, oldValue: {}, newValue: {}",
                        name,
                        type,
                        oldValue,
                        newValue);
            }
        });
    }

    @Override
    public void close() {
        this.subscription.close();
        super.close();
    }

    @Override
    public String toString() {
        return "SourcedProperty{" +
                "type=" + type +
                ", name='" + name + "'" +
                ", value=" + super.get() +
                '}';
    }
}
