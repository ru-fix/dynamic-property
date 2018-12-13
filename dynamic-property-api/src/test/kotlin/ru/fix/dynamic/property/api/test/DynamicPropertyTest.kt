package ru.fix.dynamic.property.api.test


import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.fix.dynamic.property.api.AtomicProperty
import ru.fix.dynamic.property.api.CombinedProperty
import ru.fix.dynamic.property.api.DynamicProperty
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


        val listenerAceptedValue = AtomicReference<Int>()

        property.addListener { listenerAceptedValue.set(it) }

        property.set(123)

        assertEquals(123, property.get())
        assertEquals(123, listenerAceptedValue.get())
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

        val combine = CombinedProperty(Supplier { first.get() + second.get() }, first, second)
        assertEquals("hello123", combine.get())

        first.set("hi")
        assertEquals("hi123", combine.get())

        second.set("42")
        assertEquals("hi42", combine.get())


    }
}
