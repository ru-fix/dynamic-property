package ru.fix.dynamic.property.std.source

import ru.fix.dynamic.property.api.DynamicProperty
import ru.fix.dynamic.property.api.DynamicPropertyListener
import ru.fix.dynamic.property.api.source.DynamicPropertySource
import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller
import ru.fix.dynamic.property.api.source.OptionalDefaultValue
import ru.fix.stdlib.concurrency.threads.ReferenceCleaner
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class FilePropertySource(
        private val propertiesFile: DynamicProperty<Path>,
        marshaller: DynamicPropertyMarshaller,
        referenceCleaner: ReferenceCleaner = ReferenceCleaner.getInstance()) :
        DynamicPropertySource {

    private val inMemorySource = InMemoryPropertySource(
            marshaller,
            referenceCleaner
    )

    init {

        //TODO: add FileWatcher and listen for property change

        propertiesFile.addAndCallListener { path ->
            val props = Properties().apply {
                load(Files.newBufferedReader(path, StandardCharsets.UTF_8))
            }

            val propNames = props.stringPropertyNames()
            for (key in propNames) {
                inMemorySource[key] = props.getProperty(key)
            }

            inMemorySource.propertyNames()
                    .filter { !propNames.contains(it) }
                    .forEach { name ->
                        inMemorySource.remove(name)
                    }
        }
    }

    override fun <T : Any?> subscribeAndCallListener(
            propertyName: String,
            propertyType: Class<T>,
            defaultValue: OptionalDefaultValue<T>,
            listener: DynamicPropertyListener<T>): DynamicPropertySource.Subscription =
            inMemorySource.subscribeAndCallListener(propertyName, propertyType, defaultValue, listener)

    override fun close() {
        inMemorySource.close()
    }
}