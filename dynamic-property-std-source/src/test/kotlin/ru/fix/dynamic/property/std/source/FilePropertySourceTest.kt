package ru.fix.dynamic.property.std.source

import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import ru.fix.dynamic.property.api.AtomicProperty
import ru.fix.dynamic.property.api.source.OptionalDefaultValue
import ru.fix.dynamic.property.api.source.SourcedProperty
import ru.fix.dynamic.property.jackson.JacksonDynamicPropertyMarshaller
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicReference

class FilePropertySourceTest {

    @Test
    fun `change content on path change`() {
        val f1 = Files.createTempFile("test1", ".properties").apply { toFile().deleteOnExit() }
        val f2 = Files.createTempFile("test2", ".properties").apply { toFile().deleteOnExit() }

        Files.writeString(
                f1,
                """
                |name=foo
                """.trimMargin())

        Files.writeString(
                f2,
                """
                |name=bar
                """.trimMargin())

        val path = AtomicProperty(f1)

        val source = FilePropertySource(
                sourceFilePath = path,
                marshaller = JacksonDynamicPropertyMarshaller())

        val property = SourcedProperty<String>(source, "name", String::class.java, OptionalDefaultValue.none())
        assertEquals("foo", property.get())

        val captor = AtomicReference<String>()
        property.addListener{ _, new -> captor.set(new)}

        path.set(f2)

        await().until { property.get() == "bar" }
        await().until { captor.get() == "bar" }

        f1.toFile().delete()
        f2.toFile().delete()
    }
}