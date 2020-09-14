package ru.fix.dynamic.property.std.source

import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.fix.dynamic.property.api.AtomicProperty
import ru.fix.dynamic.property.api.DynamicProperty
import ru.fix.dynamic.property.api.source.OptionalDefaultValue
import ru.fix.dynamic.property.api.source.SourcedProperty
import ru.fix.dynamic.property.jackson.MarshallerBuilder
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicReference

class FilePropertySourceTest {

    @Test
    fun `change property on file path change`() {
        val f1 = Files.createTempFile("test1", ".properties").apply { toFile().deleteOnExit() }
        val f2 = Files.createTempFile("test2", ".properties").apply { toFile().deleteOnExit() }

        Files.writeString(f1, "name=foo")
        Files.writeString(f2, "name=bar")

        val path = AtomicProperty(f1)

        val source = FilePropertySource(
            sourceFilePath = path,
            marshaller = MarshallerBuilder.createDefault()
        )

        val property = SourcedProperty(source, "name", String::class.java, OptionalDefaultValue.none())
        assertEquals("foo", property.get())

        val captor = AtomicReference<String>()
        val subscription = property
            .createSubscription()
            .setAndCallListener { _, new -> captor.set(new) }

        path.set(f2)

        await().until { property.get() == "bar" }
        await().until { captor.get() == "bar" }

        f1.toFile().delete()
        f2.toFile().delete()
    }

    @Test
    fun `change property on file content change`() {
        val f = Files.createTempFile("test1", ".properties").apply { toFile().deleteOnExit() }

        Files.writeString(f, "name=foo")

        val source = FilePropertySource(
            sourceFilePath = DynamicProperty.of(f),
            marshaller = MarshallerBuilder.createDefault()
        )

        val property = SourcedProperty(source, "name", String::class.java, OptionalDefaultValue.none())
        assertEquals("foo", property.get())

        val captor = AtomicReference<String>()
        val subscription = property.createSubscription()
            .setAndCallListener { _, new -> captor.set(new) }

        Files.writeString(f, "name=bar")

        await().until { property.get() == "bar" }
        await().until { captor.get() == "bar" }

        f.toFile().delete()
    }
}