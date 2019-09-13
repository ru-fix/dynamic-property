package ru.fix.dynamic.property.std.source

import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller
import ru.fix.stdlib.concurrency.threads.ReferenceCleaner


class InMemoryPropertySource(
        private val marshaller: DynamicPropertyMarshaller,
        private val referenceCleaner: ReferenceCleaner = ReferenceCleaner.getInstance()) :
        AbstractPropertySource(marshaller, referenceCleaner) {

    private val properties = HashMap<String, String>()

    @Synchronized
    operator fun set(key: String, value: String) {
        properties[key] = value
        invokePropertyListener(key, value)
    }

    protected override fun getPropertyValue(propertyName: String) = properties[propertyName]
}
