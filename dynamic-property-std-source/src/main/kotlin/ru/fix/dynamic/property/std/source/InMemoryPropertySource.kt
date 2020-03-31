package ru.fix.dynamic.property.std.source

import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller
import ru.fix.stdlib.concurrency.threads.ReferenceCleaner

/**
 * Keep properties in memory.
 * All listeners will be invoked during [set] invocation within same thread.
 */
open class InMemoryPropertySource(
        marshaller: DynamicPropertyMarshaller,
        referenceCleaner: ReferenceCleaner = ReferenceCleaner.getInstance()) :
        AbstractPropertySource(marshaller, referenceCleaner) {

    private val properties = HashMap<String, String>()

    @Synchronized
    operator fun set(key: String, value: String) {
        properties[key] = value
        invokePropertyListener(key, value)
    }

    @Synchronized
    fun remove(key: String) {
        properties.remove(key)
        invokePropertyListener(key, null)
    }

    fun propertyNames(): Set<String> = properties.keys

    protected override fun getPropertyValue(propertyName: String) = properties[propertyName]
}
