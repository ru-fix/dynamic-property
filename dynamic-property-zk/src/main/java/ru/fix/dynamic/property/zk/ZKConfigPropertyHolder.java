package ru.fix.dynamic.property.zk;

import org.slf4j.Logger;
//import ru.fix.aggregating.profiler.NoopProfiler;
import ru.fix.dynamic.property.api.DynamicProperty;
import ru.fix.dynamic.property.api.DynamicPropertyListener;
import ru.fix.dynamic.property.api.DynamicPropertySource;
//import ru.fix.stdlib.concurrency.threads.NamedExecutors;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Holder class to keep property value
 */
public class ZKConfigPropertyHolder<T> implements DynamicProperty<T> {

    private static final Logger log = getLogger(ZKConfigPropertyHolder.class);
//    private static final ExecutorService executor = Executors.newDynamicPool(
//            //Temp solution, will be replaced by https://jira.fix.ru/browse/CPAPSM-9317
//            "zk_property-holder-thread-pool", DynamicProperty.of(4), new NoopProfiler());

    //TODO: что будем использовать для получения
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    private DynamicPropertySource zkConfig;
    private Class<T> type;
    private String name;
    private T defaultValue;
    private volatile T currentValue;

    private List<DynamicPropertyListener<T>> listeners = new CopyOnWriteArrayList<>();

    public ZKConfigPropertyHolder(ZkPropertySource zkConfig, String name, Class<T> type) {
        this(zkConfig, name, type, null);
    }

    public ZKConfigPropertyHolder(DynamicPropertySource zkConfig, String name, Class<T> type, T defaultValue) {
        this.zkConfig = zkConfig;
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;

        init();
    }

    private void init() {
        zkConfig.addPropertyChangeListener(
                name,
                newValue -> {
                    currentValue = ValueConverter.convert(type, newValue, defaultValue);
                    listeners.forEach(listener -> executor.submit(() -> {
                        try {
                            listener.onPropertyChanged(currentValue);
                        } catch (Exception e) {
                            log.error("Failed to update property {} with value {}", name, newValue, e);
                        }
                    }));
                });
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
    public void addListener(DynamicPropertyListener<T> listener) {
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
