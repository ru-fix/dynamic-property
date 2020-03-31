package ru.fix.dynamic.property.polling;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.dynamic.property.api.AtomicProperty;
import ru.fix.dynamic.property.api.DynamicProperty;
import ru.fix.stdlib.concurrency.threads.ReschedulableScheduler;
import ru.fix.stdlib.concurrency.threads.Schedule;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Creates {@link DynamicProperty} backed up by {@link Supplier}.<br>
 * Single thread will regularly pool data from {@link Supplier} and will notify {@link DynamicProperty} listeners
 * about property change if any.<br>
 * Be aware that such {@link DynamicProperty} instance could miss changes that could occur behind the {@link Supplier}.<br>
 * Suppose that {@link DynamicPropertyPoller} polls {@link Supplier} each 10 minute. <br>
 * At 10:00 {@link DynamicPropertyPoller} gets value 120.<br>
 * At 10:03 the source behind {@link Supplier} changes value from 123 to 140<br>
 * At 10:10 {@link DynamicPropertyPoller} gets value 140 and notifies {@link DynamicProperty} listeners about the change.<br>
 * At 10:14 the source behind {@link Supplier} changes value from 140 to 175<br>
 * At 10:16 the source behind {@link Supplier} changes value from 175 back to 140<br>
 * At 10:20 {@link DynamicPropertyPoller} gets value 140 and does not notifies anyone.<br>
 */
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

    /**
     * @see DynamicPropertyPoller#DynamicPropertyPoller(ReschedulableScheduler, DynamicProperty, DynamicProperty)
     */
    public DynamicPropertyPoller(ReschedulableScheduler scheduler,
                                 DynamicProperty<Schedule> pollingSchedule) {
        this(scheduler, pollingSchedule, DynamicProperty.of(Duration.of(5, ChronoUnit.MINUTES)));
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
