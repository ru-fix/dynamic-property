package ru.fix.dynamic.property.std.source

import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller
import ru.fix.dynamic.property.api.source.DynamicPropertySource
import ru.fix.dynamic.property.api.source.OptionalDefaultValue
import ru.fix.stdlib.reference.ReferenceCleaner
import java.util.concurrent.ConcurrentHashMap

/**
 * Keep properties in memory.
 * All listeners will be invoked during [set] invocation within same thread.
 */
open class InMemoryPropertySource(
        marshaller: DynamicPropertyMarshaller,
        referenceCleaner: ReferenceCleaner = ReferenceCleaner.getInstance()) : DynamicPropertySource {

    private val properties = ConcurrentHashMap<String, String>()

    private val propertySourcePublisher = PropertySourcePublisher(
            propertySourceReader = object : PropertySourceReader {
                override fun getPropertyValue(propertyName: String): String? {
                    return properties[propertyName]
                }
            },
            marshaller = marshaller,
            referenceCleaner = referenceCleaner
    )

    operator fun set(key: String, value: String) {
        properties[key] = value
        propertySourcePublisher.notifyAboutPropertyChange(key, value)
    }

    fun remove(key: String) {
        properties.remove(key)
        propertySourcePublisher.notifyAboutPropertyChange(key, null)
    }

    fun propertyNames(): Set<String> = properties.keys

    override fun <T : Any?> createSubscription(
            propertyName: String,
            propertyType: Class<T>,
            defaultValue: OptionalDefaultValue<T>): DynamicPropertySource.Subscription<T> =
            propertySourcePublisher.createSubscription(propertyName, propertyType, defaultValue)

    override fun close() {
        properties.clear()
        propertySourcePublisher.close()
    }
}
