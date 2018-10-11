package ru.fix.dynamic.property.polling;

/**
 *
 * @author Andrey Kiselev
 */

import ru.fix.dynamic.property.api.DynamicProperty;
import ru.fix.stdlib.concurrency.threads.ReschedulableScheduler;
import ru.fix.stdlib.concurrency.threads.Schedule;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public class DynamicPropertyPoller implements AutoCloseable {
    private Map<PolledProperty, Supplier> properties = new WeakHashMap<>();
    private ReschedulableScheduler scheduler;
    private DynamicProperty<Schedule> delay;

    public DynamicPropertyPoller(ReschedulableScheduler scheduler,
                                 DynamicProperty<Schedule> delay) {
        this.scheduler = scheduler;
        this.delay = delay;
        
        this.delay.addListener(newRate -> {
                this.scheduler.shutdown();
                this.scheduler.schedule(
                    () -> newRate,
                    0,
                    this::pollAll);
            });

        this.scheduler.schedule(
            delay,
            0,
            this::pollAll);
    }

    @Override
    public void close() {
        this.scheduler.shutdown();
        synchronized(properties) {
            this.properties.clear();
        }
    }
    
    private void pollAll() {
        synchronized(properties) {
            properties.forEach((k, v) ->  k.poll());
        }
    }
    
    public <DType> PolledProperty<DType> createProperty(Supplier<DType> retriever) {
        PolledProperty<DType> property = new PolledProperty<>(retriever);
        property.poll();
        synchronized(properties) {
            properties.put(property, retriever);
        }
        return property;
    }

    public void deleteProperty(PolledProperty pp) {
        synchronized(properties) {
            properties.remove(pp);
        }
    }
}
