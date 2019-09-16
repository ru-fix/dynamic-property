package ru.fix.dynamic.property.polling;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.dynamic.property.api.AtomicProperty;
import ru.fix.dynamic.property.api.DynamicProperty;
import ru.fix.stdlib.concurrency.threads.ReschedulableScheduler;
import ru.fix.stdlib.concurrency.threads.Schedule;

import java.time.Duration;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class DynamicPropertyPoller implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(DynamicPropertyPoller.class);

    private WeakHashMap<AtomicProperty, Supplier> properties = new WeakHashMap<>();
    private ReschedulableScheduler scheduler;
    private DynamicProperty<Duration> shutdownDelay;

    /**
     * @param scheduler
     * @param pollingSchedule how often properties values will be polled by retrievers
     * @param shutdownTimeout how log {@link #close()} will wait for polling process to stop
     */
    public DynamicPropertyPoller(ReschedulableScheduler scheduler,
                                 DynamicProperty<Schedule> pollingSchedule,
                                 DynamicProperty<Duration> shutdownTimeout) {
        this.scheduler = scheduler;
        this.scheduler.schedule(
                pollingSchedule,
                0,
                this::pollAll);
        this.shutdownDelay = shutdownTimeout;
    }

    @Override
    public void close() {
        synchronized (properties) {
            this.properties.clear();
        }
        this.scheduler.shutdown();
        try {
            if (!this.scheduler.awaitTermination(shutdownDelay.get().toMillis(), TimeUnit.MILLISECONDS)) {
                log.error("Failed to await termination for {}", shutdownDelay.get());
                this.scheduler.shutdownNow();
            }
        } catch (InterruptedException exc) {
            log.error("Failed to properly close poller", exc);
        }
    }

    private void pollAll() {
        synchronized (properties) {
            properties.forEach((property, retriever) -> {
                retrieveAndUpdatePropertyValue(property, retriever);
            });
        }
    }

    private void retrieveAndUpdatePropertyValue(AtomicProperty property, Supplier retriever) {
        try {
            Object value = retriever.get();
            property.set(value);
        } catch (Exception exc) {
            log.error("Failed to retrieve and update property", exc);
        }
    }

    class PolledProperty<T> extends AtomicProperty<T> {
        @Override
        public void close() {
            deleteProperty(this);
            super.close();
        }
    }

    public <T> DynamicProperty<T> createProperty(Supplier<T> retriever) {
        PolledProperty<T> property = new PolledProperty<T>();

        synchronized (properties) {
            properties.put(property, retriever);
        }

        retrieveAndUpdatePropertyValue(property, retriever);

        return property;
    }

    public void deleteProperty(DynamicProperty polledProperty) {
        synchronized (properties) {
            properties.remove(polledProperty);
        }
    }
}
