package ru.fix.dynamic.property.api

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import ru.fix.stdlib.reference.GarbageGenerator
import ru.fix.stdlib.reference.ReferenceCleaner
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class DynamicPropertyTest {

    private val garbageGenerator = GarbageGenerator()
        .setGarbageSizePerIterationMB(10)
        .setDelay(Duration.ofMillis(100))
        .setTimeout(Duration.ofMinutes(1))

    private val referenceCleaner = ReferenceCleaner.getInstance()

    class MyService(poolSize: DynamicProperty<Int>) {
        private val poolSize: PropertySubscription<Int> = poolSize
            .createSubscription()
            .setAndCallListener { oldValue, newValue ->
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

        val subscription = property.createSubscription().setAndCallListener { old, new ->
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

        val subscription = intProperty
            .createSubscription()
            .setAndCallListener { old, new ->
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
    fun `map method release weakly reachable listeners and does not lead to OOM`() {
        val property = AtomicProperty(12)

        fun functionTakesPropertyButDoesNotKeepReferenceToIt(setting: DynamicProperty<String>): Int {
            return setting.get().length
        }

        var mappedProperty = property.map { it.toString() }

        functionTakesPropertyButDoesNotKeepReferenceToIt(mappedProperty)

        val referenceWasCleaned = AtomicBoolean(false)
        referenceCleaner.register(mappedProperty, null) { _, _ ->
            referenceWasCleaned.set(true)
        }
        mappedProperty = null

        val result = garbageGenerator.generateGarbageAndWaitForCondition {
            referenceWasCleaned.get()
        }
        assertTrue(result)
    }

    /**
     * ```
     * ~root~
     *    ↑
     *    │
     *    │ .map
     *    │            subscription
     * ~child1~ ←————————————————————————╮
     *    ↑                              │
     *    │ .map                      ~some_object~
     *    │        implicit dependency   ╎
     * ~child2~ ←╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╯
     *               in subscription
     * ```
     */
    @RepeatedTest(20)
    fun `mapped properties dependency graph`() {
        val root = AtomicProperty(0L)
        val child1 = root.map { it }
        val child2 = child1.map { it }

        // some business object with explicit dependency to child1 and implicit dependency to child2
        child1.createSubscription().setAndCallListener { _, newValue ->
            val child2Val = child2.get()
            assertEquals(newValue, child2Val)
        }

        root.set(1)
    }

    @RepeatedTest(20)
    fun `delegated properties dependency graph`() {
        val root = AtomicProperty(0L)
        val child1 = root.map { it }
        val child2 = DynamicProperty.delegated { child1.get() }

        // some business object with explicit dependency to child1 and implicit dependency to child2
        child1.createSubscription().setAndCallListener { _, newValue ->
            val child2Val = child2.get()
            assertEquals(newValue, child2Val)
        }

        root.set(1)
    }
}
