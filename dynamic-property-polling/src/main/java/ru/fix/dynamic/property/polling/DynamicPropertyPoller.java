package ru.fix.dynamic.property.polling;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.dynamic.property.api.AtomicProperty;
import ru.fix.dynamic.property.api.DynamicProperty;
import ru.fix.stdlib.concurrency.threads.ReschedulableScheduler;
import ru.fix.stdlib.concurrency.threads.Schedule;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentLinkedDeque;
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

    private final ConcurrentLinkedDeque<WeakReference<PolledProperty>> createdPolledProperties =
            new ConcurrentLinkedDeque<>();
    private final ReschedulableScheduler scheduler;
    private final DynamicProperty<Duration> shutdownDelay;

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
                this::pollAllAndExpungeStale);
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
        this.createdPolledProperties.clear();
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

    private void pollAllAndExpungeStale() {
        createdPolledProperties.removeIf(ref -> ref.get() == null);

        createdPolledProperties.forEach(ref -> {
            DynamicPropertyPoller.PolledProperty property = ref.get();
            if (property != null) {
                property.retrieveAndUpdatePropertyValue();
            }
        });
    }


    class PolledProperty<T> extends AtomicProperty<T> {

        private final Supplier<T> retriever;
        private WeakReference<PolledProperty<T>> reference;

        PolledProperty(Supplier<T> retriever) {
            this.retriever = retriever;
        }

        private void retrieveAndUpdatePropertyValue() {
            try {
                T value = retriever.get();
                this.set(value);
            } catch (Exception exc) {
                log.error("Failed to retrieve and update property", exc);
            }
        }


        @Override
        public void close() {
            deleteProperty(this);
            super.close();
        }
    }

    /**
     * @param retriever
     * @param <T>
     * @return
     * @see #deleteProperty(DynamicProperty)
     */
    public <T> DynamicProperty<T> createProperty(Supplier<T> retriever) {
        PolledProperty<T> property = new PolledProperty<T>(retriever);
        property.reference = new WeakReference<>(property);
        createdPolledProperties.add((WeakReference<PolledProperty>) (WeakReference) property.reference);
        property.retrieveAndUpdatePropertyValue();
        return property;
    }

    /**
     * @param polledProperty DynamicProperty created by this {@link DynamicPropertyPoller}
     */
    public void deleteProperty(DynamicProperty polledProperty) {
        createdPolledProperties.remove(((PolledProperty) polledProperty).reference);
    }
}
