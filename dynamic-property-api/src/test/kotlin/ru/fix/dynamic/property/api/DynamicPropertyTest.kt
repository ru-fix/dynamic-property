package ru.fix.dynamic.property.api


import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier

class DynamicPropertyTest {

    @Test
    fun constant_property() {

        val property = DynamicProperty.of(122)
        assertEquals(122, property.get())
    }

    @Test
    fun atomic_property() {

        val property = AtomicProperty(122)
        assertEquals(122, property.get())


        val listenerAcceptedValue = AtomicReference<Int>()

        property.addListener { listenerAcceptedValue.set(it) }

        property.set(123)

        assertEquals(123, property.get())
        assertEquals(123, listenerAcceptedValue.get())
    }

    @Test
    fun mapped_property() {

        val stringProperty = AtomicProperty("159")

        val intProperty = stringProperty.map { str -> str.toInt() }

        assertEquals(159, intProperty.get())

        val captor = AtomicReference(0)
        intProperty.addListener { captor.set(it) }


        stringProperty.set("305")

        assertEquals(305, captor.get())
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
}
