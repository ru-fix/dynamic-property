package ru.fix.dynamic.property.api.test

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.fix.dynamic.property.api.DynamicProperty
import ru.fix.dynamic.property.polling.DynamicPropertyPoller
import ru.fix.stdlib.concurrency.threads.ReschedulableScheduler
import ru.fix.stdlib.concurrency.threads.Schedule
import java.util.concurrent.atomic.AtomicReference

/**
 * test for DynamicPropertyPoller
 */
class DynamicPropertyTest {

    /**
     * basic test
     */
    @Test
    fun polled_property() {
        val scheduler = mockk<ReschedulableScheduler>(relaxed = true)
        val pollingTask = slot<Runnable>()
        every {
            scheduler.schedule(any(), any(), capture(pollingTask))
        } returns mockk()

        val poller = DynamicPropertyPoller(
            scheduler,
            DynamicProperty.of(Schedule.withRate(1L))
        )

        assertTrue(pollingTask.isCaptured)

        val valueHolder = AtomicReference("start")

        val property = poller.createProperty { valueHolder.get() }
        assertEquals("start", property.get())

        valueHolder.set("work")
        pollingTask.captured.run()
        assertEquals("work", property.get())

        property.close()
        valueHolder.set("end")
        pollingTask.captured.run()
        assertEquals("work", property.get())

        poller.close()
    }
}
