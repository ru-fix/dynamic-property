package ru.fix.dynamic.property.api.test


import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.fix.dynamic.property.polling.DynamicPropertyPoller
import ru.fix.dynamic.property.api.DynamicProperty
import ru.fix.aggregating.profiler.NoopProfiler;
import ru.fix.stdlib.concurrency.threads.NamedExecutors;
import ru.fix.stdlib.concurrency.threads.Schedule;

/**
 * test for DynamicPropertyPoller
 */
class DynamicPropertyTest {

    /**
     * basic test
     */
    @Test
    fun polled_property() {
        var value = "start"
        val poller = DynamicPropertyPoller(
            NamedExecutors.newSingleThreadScheduler(
                "Polling",
                NoopProfiler()),
            DynamicProperty.of(
                Schedule(Schedule.Type.RATE,
                         1L)))
        
        poller.init()
        val property = poller.createProperty{value} 
        assertEquals("start", property.get())

        value = "work"
        Thread.sleep(100) 
        assertEquals("work", property.get())

        poller.deleteProperty(property)
        value = "end"
        Thread.sleep(100)         
        assertEquals("work", property.get())

        poller.close()
    }
}
