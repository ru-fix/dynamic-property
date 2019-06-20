package ru.fix.dynamic.property.api


import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference

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
}