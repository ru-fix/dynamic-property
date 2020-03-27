package ru.fix.dynamic.property.api


import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import ru.fix.stdlib.reference.ReferenceCleaner
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier

class DynamicPropertyTest {

    class MyService(poolSize: DynamicProperty<Int>) {
        private val poolSize: PropertySubscription<Int> = poolSize.subscribeAndCall(this) { oldValue, newValue ->
            println("poolSize changed from $oldValue to $newValue")
        }

        fun doWork() {
            println("doWork with poolSize: ${poolSize.get()}")
        }
    }

    @Test
    fun constant_property() {
        val property = DynamicProperty.of(122)
        assertEquals(122, property.get())
    }

    @Test
    fun atomic_property() {
        val property = AtomicProperty(122)
        assertEquals(122, property.get())


        val listenerAcceptedNewValue = AtomicReference<Int>()
        val listenerAcceptedOldValue = AtomicReference<Int>()

        val subscription = property.subscribeAndCall(null) { old, new ->
            listenerAcceptedOldValue.set(old)
            listenerAcceptedNewValue.set(new)
        }

        property.set(123)

        assertEquals(123, property.get())
        assertEquals(122, listenerAcceptedOldValue.get())
        assertEquals(123, listenerAcceptedNewValue.get())
    }

    @Test
    fun mapped_property() {

        val stringProperty = AtomicProperty("159")

        val intProperty = stringProperty.map { str -> str.toInt() }

        assertEquals(159, intProperty.get())

        val captorOld = AtomicReference(0)
        val captorNew = AtomicReference(0)

        val subscription = intProperty.subscribeAndCall(null) { old, new ->
            captorOld.set(old)
            captorNew.set(new)
        }

        stringProperty.set("305")

        assertEquals(305, captorNew.get())
        assertEquals(159, captorOld.get())
    }

    @Test
    fun `combine properties`() {
        val first = AtomicProperty("hello")
        val second = AtomicProperty("123")

        val combine = CombinedProperty(listOf(first, second)) { first.get() + second.get() }
        assertEquals("hello123", combine.get())

        first.set("hi")
        assertEquals("hi123", combine.get())

        second.set("42")
        assertEquals("hi42", combine.get())
    }

    @Test
    fun `delegated property`() {
        val property = DynamicProperty.delegated { 12 }
        assertEquals(12, property.get())
    }

    @Test
    fun `constant property of null`() {
        val property = DynamicProperty.of<String>(null)
        assertNull(property.get())
    }

    @Test
    fun `to string`() {
        assertEquals("AtomicProperty(12)", AtomicProperty(12).toString())
    }

    @Test
    fun `map method does not lead to OOM`() {
        val property = AtomicProperty(12)

        fun functionTakesPropertyButDoesNotKeepReferenceOnIt(setting: DynamicProperty<String>): Int {
            return setting.get().length
        }

        //TODO: add gc generator to the stdlib library
        //TODO: fix looop
        var mappedProperty = property.map { it.toString() }

        functionTakesPropertyButDoesNotKeepReferenceOnIt(mappedProperty)

        val referenceWasCleaned = AtomicBoolean(false)
        ReferenceCleaner.getInstance().register(mappedProperty, null) { ref, meta ->
            referenceWasCleaned.set(true)
        }
        mappedProperty = null

        val result = generateGarbageAndWaitForCondition(Duration.ofMinutes(10), Supplier{
            referenceWasCleaned.get()
        })
        assertTrue(result)


    }


    @Test
    fun `listeners do not lead to OOM`() {
        val property = AtomicProperty(12)

        var listener: PropertyListener<Int>? = PropertyListener<Int>{ oldValue, newValue -> println("$oldValue -> $newValue")}

        property.subscribeAndCall(null, listener!!)


        val referenceWasCleaned = AtomicBoolean(false)
        ReferenceCleaner.getInstance().register(listener, null) { ref, meta ->
            referenceWasCleaned.set(true)
        }
        listener = null

        val result = generateGarbageAndWaitForCondition(Duration.ofMinutes(10), Supplier{
            referenceWasCleaned.get()
        })
        assertTrue(result)


    }


    @Test
    fun `listeners will be under gc`() {
        val property = AtomicProperty(12)

        var listener: PropertyListener<Int>? = PropertyListener<Int>{ oldValue, newValue -> println("$oldValue -> $newValue")}

        val referenceWasCleaned = AtomicBoolean(false)
        ReferenceCleaner.getInstance().register(listener, null) { ref, meta ->
            referenceWasCleaned.set(true)
        }
        listener = null

        val result = generateGarbageAndWaitForCondition(Duration.ofMinutes(10), Supplier{
            referenceWasCleaned.get()
        })
        assertTrue(result)


    }

    @Test
    fun `listeners will be under gc by OBJ`() {
        val property = AtomicProperty(12)

        var obj:Object? = Object()

        val referenceWasCleaned = AtomicBoolean(false)
        ReferenceCleaner.getInstance().register(obj, null) { ref, meta ->
            referenceWasCleaned.set(true)
        }
        obj = null

        val result = generateGarbageAndWaitForCondition(Duration.ofMinutes(10), Supplier{
            referenceWasCleaned.get()
        })
        assertTrue(result)


    }


    private fun generateGarbageAndWaitForCondition(duration: Duration, condition: Supplier<Boolean>): Boolean {
        val data = ArrayList<Any>()
        val start = System.currentTimeMillis()
        while (!condition.get() && System.currentTimeMillis() - start <= duration.toMillis()) {
            println("Running time: " + Duration.of(System.currentTimeMillis() - start, ChronoUnit.MILLIS))
            println("Occupied memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 + " Kb")
            for (mb in 0..9) {
                for (kb in 0..1023) {
                    val obj = IntArray(1024)
                    data.add(obj)
                }
            }
            data.clear()
            Thread.sleep(500)
        }
        return condition.get()
    }

}
