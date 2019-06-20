package ru.fix.dynamic.property.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Contain property initial value. Automatically register property change listener.
 */
public class DefaultDynamicProperty<T> implements DynamicProperty<T> {

    private static final Logger log = LoggerFactory.getLogger(DefaultDynamicProperty.class);

    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    private DynamicPropertySource zkConfig;
    private Class<T> type;
    private String name;
    private T defaultValue;
    private volatile T currentValue;

    private List<DynamicPropertyChangeListener<T>> listeners = new CopyOnWriteArrayList<>();

    public DefaultDynamicProperty(DynamicPropertySource zkConfig, String name, Class<T> type) {
        this(zkConfig, name, type, null);
    }

    public DefaultDynamicProperty(DynamicPropertySource zkConfig, String name, Class<T> type, T defaultValue) {
        this.zkConfig = zkConfig;
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;

        init();
    }

    private void init() {
        zkConfig.addPropertyChangeListener(
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
        currentValue = zkConfig.getProperty(name, type, defaultValue);
    }

    @Override
    public T get() {
        return currentValue;
    }

    /**
     * WARNING
     * Listener runs in internal zookeeper thread. It should be very light, run very fast and so not use locks.
     *
     * @param listener
     * @return
     */
    @Override
    public void addListener(DynamicPropertyChangeListener<T> listener) {
        listeners.add(listener);
    }

    @Override
    public String toString() {
        return "ZKConfigPropertyHolder{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", currentValue=" + currentValue +
                '}';
    }
}
