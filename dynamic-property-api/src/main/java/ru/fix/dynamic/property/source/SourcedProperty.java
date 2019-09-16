package ru.fix.dynamic.property.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.dynamic.property.api.DynamicProperty;
import ru.fix.dynamic.property.api.DynamicPropertyListener;
import ru.fix.dynamic.property.api.DynamicPropertySource;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Contains property initial value.
 * Registers listener within {@link DynamicPropertySource}.
 * Listen for events from  {@link DynamicPropertySource}.
 * Update local value and propagates update events to it's subscribes.
 * See {@link DynamicProperty#addAndCallListener(DynamicPropertyListener)}
 *
 * If instance of this class became weakly reachable it will stop receiving events from {@link DynamicPropertySource}
 * Same effect will be archived through {@link DynamicProperty#close()}
 */
public class SourcedProperty<T> implements DynamicProperty<T> {

    private static final Logger log = LoggerFactory.getLogger(SourcedProperty.class);

    private final String name;
    private final Class<T> type;
    private final AtomicReference<T> currentValue = new AtomicReference<>();
    private final List<DynamicPropertyListener<T>> listeners = new CopyOnWriteArrayList<>();

    private final DynamicPropertySource propertySource;
    private final DynamicPropertySource.Subscription subscription;

    public SourcedProperty(DynamicPropertySource propertySource,
                           String name,
                           Class<T> type,
                           DefaultValue<T> defaultValue) {
        this.name = name;
        this.type = type;
        this.propertySource = propertySource;

        subscription = propertySource.subscribeAndCallListener(
                this.name,
                this.type,
                defaultValue,
                newValue -> {
                    synchronized (SourcedProperty.this) {
                        currentValue.set(newValue);
                        listeners.forEach(listener -> {
                            try {
                                listener.onPropertyChanged(newValue);
                            } catch (Exception e) {
                                log.error("Failed to update property {} with value {}", this.name, newValue, e);
                            }
                        });
                    }
                }
        );
    }

    @Override
    public T get() {
        return currentValue.get();
    }

    /**
     * Listener callback runs in the {@link DynamicPropertySource} thread.
     *
     * @param listener Listener runs whenever property value changes.
     */
    @Override
    public DynamicProperty<T> addListener(DynamicPropertyListener<T> listener) {
        listeners.add(listener);
        return this;
    }

    @Override
    public synchronized DynamicProperty<T> addAndCallListener(DynamicPropertyListener<T> listener) {
        listeners.add(listener);
        listener.onPropertyChanged(currentValue.get());
        return this;
    }

    @Override
    public synchronized T addListenerAndGet(DynamicPropertyListener<T> listener) {
        listeners.add(listener);
        return currentValue.get();
    }

    @Override
    public DynamicProperty<T> removeListener(DynamicPropertyListener<T> listener) {
        listeners.remove(listener);
        return this;
    }

    @Override
    public void close() {
        this.subscription.close();
        listeners.clear();
    }

    @Override
    public String toString() {
        return "SourcedProperty{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", currentValue=" + currentValue +
                '}';
    }
}
