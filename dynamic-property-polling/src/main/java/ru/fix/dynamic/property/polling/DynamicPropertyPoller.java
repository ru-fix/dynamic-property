package ru.fix.dynamic.property.polling;

/**
 *
 * @author Andrey Kiselev
 */

import java.util.WeakHashMap;
import java.util.function.Supplier;
import javax.annotation.PostConstruct;

import ru.fix.dynamic.property.api.DynamicProperty;
import ru.fix.stdlib.concurrency.threads.ReschedulableScheduler;
import ru.fix.stdlib.concurrency.threads.Schedule;

public class DynamicPropertyPoller {
    private WeakHashMap<PolledProperty, Supplier> properties = new WeakHashMap<>();
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
    }

    @PostConstruct
    public void init() {
        this.scheduler.schedule(
            () -> delay.get(),
            0,
            this::pollAll);
    }

    public void close() {
        this.scheduler.shutdown();
    }
    
    private void pollAll() {
        properties.forEach((k, v) ->  k.poll());
    }
    
    public <DType> PolledProperty<DType> createProperty(Supplier<DType> retriever) {
        PolledProperty<DType> property = new PolledProperty<>(retriever);
        property.poll();
        properties.put(property, retriever);
        
        return property;
    }

    public void deleteProperty(PolledProperty pp) {
        properties.remove(pp);
    }
}
