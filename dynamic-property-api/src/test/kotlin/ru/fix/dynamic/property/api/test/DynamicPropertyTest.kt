package ru.fix.dynamic.property.api.test


import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.fix.dynamic.property.api.AtomicProperty
import ru.fix.dynamic.property.api.DynamicProperty
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
}