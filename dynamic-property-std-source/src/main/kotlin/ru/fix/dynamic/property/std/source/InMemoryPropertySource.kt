package ru.fix.dynamic.property.std.source

import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller
import ru.fix.dynamic.property.api.source.DynamicPropertySource
import ru.fix.dynamic.property.api.source.OptionalDefaultValue
import ru.fix.stdlib.reference.ReferenceCleaner
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Keep properties in memory.
 * All listeners will be invoked during [set] invocation within same thread.
 */
open class InMemoryPropertySource(
        marshaller: DynamicPropertyMarshaller,
        referenceCleaner: ReferenceCleaner = ReferenceCleaner.getInstance()) : DynamicPropertySource {

    private val readAndChangeLock = ReentrantLock()

    private val properties = HashMap<String, String>()

    private val propertySourcePublisher = PropertySourcePublisher(
            propertySourceAccessor = object : PropertySourceAccessor {
                override fun accessPropertyUnderLock(propertyName: String, accessor: (String?) -> Unit) {
                    readAndChangeLock.withLock {
                        accessor(properties[propertyName])
                    }
                }
            },
            marshaller = marshaller,
            referenceCleaner = referenceCleaner
    )

    operator fun set(key: String, value: String) {
        readAndChangeLock.withLock {
            properties[key] = value
            propertySourcePublisher.notifyAboutPropertyChange(key, value)
        }
    }

    fun remove(key: String) {
        readAndChangeLock.withLock {
            properties.remove(key)
            propertySourcePublisher.notifyAboutPropertyChange(key, null)
        }
    }

    fun propertyNames(): Set<String> = properties.keys

    override fun <T : Any?> createSubscription(
            propertyName: String,
            propertyType: Class<T>,
            defaultValue: OptionalDefaultValue<T>): DynamicPropertySource.Subscription<T> =
            propertySourcePublisher.createSubscription(propertyName, propertyType, defaultValue)

    override fun close() {
        propertySourcePublisher.close()
    }
}
