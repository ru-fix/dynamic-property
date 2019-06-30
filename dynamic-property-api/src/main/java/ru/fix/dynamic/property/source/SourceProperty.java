package ru.fix.dynamic.property.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.dynamic.property.api.DynamicProperty;
import ru.fix.dynamic.property.api.DynamicPropertyListener;
import ru.fix.dynamic.property.api.DynamicPropertySource;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Contain property initial value. Automatically register property change listener.
 */
public class SourceProperty<T> implements DynamicProperty<T> {

    private static final Logger log = LoggerFactory.getLogger(SourceProperty.class);

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private DynamicPropertySource propertySource;
    private Class<T> type;
    private String name;
    private T defaultValue;
    private volatile T currentValue;

    private List<DynamicPropertyListener<T>> listeners = new CopyOnWriteArrayList<>();

    public SourceProperty(DynamicPropertySource propertySource, String name, Class<T> type) {
        this(propertySource, name, type, null);
    }

    public SourceProperty(DynamicPropertySource propertySource, String name, Class<T> type, T defaultValue) {
        this.propertySource = propertySource;
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;

        init();
    }

    private void init() {
        propertySource.addPropertyChangeListener(
                name,
                type,
                newValue -> {
                    currentValue = newValue;
                    listeners.forEach(listener -> executor.submit(() -> {
                        try {
                            listener.onPropertyChanged(newValue);
                        } catch (Exception e) {
                            log.error("Failed to update property {} with value. {}", name, newValue, e);
                        }
                    }));
                }
        );
        currentValue = propertySource.getProperty(name, type, defaultValue);
    }

    @Override
    public T get() {
        return currentValue;
    }

    @Override
    public void addListener(DynamicPropertyListener<T> listener) {
        listeners.add(listener);
    }

    @Override
    public String toString() {
        return "SourceProperty{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", currentValue=" + currentValue +
                '}';
    }
}
