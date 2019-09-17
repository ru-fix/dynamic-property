package ru.fix.dynamic.property.std.source

import org.slf4j.LoggerFactory
import ru.fix.dynamic.property.api.DynamicPropertyListener
import ru.fix.dynamic.property.api.DynamicPropertySource
import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller
import ru.fix.dynamic.property.source.DefaultValue
import ru.fix.dynamic.property.source.DynamicPropertyNotFoundException
import ru.fix.stdlib.concurrency.threads.CleanableWeakReference
import ru.fix.stdlib.concurrency.threads.ReferenceCleaner
import java.util.concurrent.ConcurrentHashMap


abstract class AbstractPropertySource(
        private val marshaller: DynamicPropertyMarshaller,
        private val referenceCleaner: ReferenceCleaner = ReferenceCleaner.getInstance()) : DynamicPropertySource {

    companion object {
        private val log = LoggerFactory.getLogger(AbstractPropertySource::class.java)
    }

    private inner class Subscription(
            val propertyName: String,
            val propertyType: Class<Any>,
            val defaultValue: DefaultValue<*>,
            val listener: DynamicPropertyListener<Any>) : DynamicPropertySource.Subscription {

        lateinit var cleanableReference: CleanableWeakReference<Subscription>

        override fun close() {
            Subscriptions.removeSubRef(propertyName, cleanableReference)
        }
    }

    private object Subscriptions {
        val store = ConcurrentHashMap<String, ConcurrentHashMap<CleanableWeakReference<Subscription>, Unit>>()

        fun addSubRef(propertyName: String, subRef: CleanableWeakReference<Subscription>) =
                store.compute(propertyName) { _, value ->
                    (value ?: ConcurrentHashMap()).apply {
                        put(subRef, Unit)
                    }
                }

        fun removeSubRef(propertyName: String, subRef: CleanableWeakReference<Subscription>) =
                store.compute(propertyName) { _, value ->
                    if (value != null) {
                        value.remove(subRef)
                        if (value.isEmpty()) {
                            null
                        } else {
                            value
                        }
                    } else {
                        null
                    }
                }

        operator fun get(propertyName: String) = store[propertyName]

        fun removeAllSubRef() {
            val propIter = store.iterator()
            while (propIter.hasNext()) {
                val subIter = propIter.next().value.iterator()
                while (subIter.hasNext()) {
                    val (ref, _) = subIter.next()
                    ref.cancelCleaningOrder()
                    subIter.remove()
                }
                propIter.remove()
            }
        }
    }

    /**
     * @param newSerializedValue if null then property will be changed to default value
     */
    protected fun invokePropertyListener(propertyName: String, newSerializedValue: String?) {
        Subscriptions[propertyName]?.forEach { subRef, _ ->
            subRef.get()?.let { sub ->
                try {
                    val newValue = extractPropertyValueOrDefault<Any?>(newSerializedValue, sub)
                    sub.listener.onPropertyChanged(newValue)
                } catch (exc: Exception) {
                    log.error("Failed to update property {}", propertyName, exc)
                }
            }
        }
    }

    /**
     * @return null if there are no such property in PropertySource
     */
    protected abstract fun getPropertyValue(propertyName: String): String?


    @Synchronized
    override fun <T> subscribeAndCallListener(
            propertyName: String,
            propertyType: Class<T>,
            defaultValue: DefaultValue<T>,
            listener: DynamicPropertyListener<T>): DynamicPropertySource.Subscription {

        val subscription = Subscription(
                propertyName,
                propertyType as Class<Any>,
                defaultValue,
                listener as DynamicPropertyListener<Any>)

        val subRef = referenceCleaner.register(subscription, propertyName) { a_ref, a_propName ->
            Subscriptions.removeSubRef(a_propName, a_ref)
        }
        subscription.cleanableReference = subRef

        Subscriptions.addSubRef(propertyName, subRef)

        val value = extractPropertyValueOrDefault<T>(getPropertyValue(propertyName), subscription)
        listener.onPropertyChanged(value)

        return subscription
    }

    private fun <T : Any?> extractPropertyValueOrDefault(serialisedPropertyValue: String?, subscription: Subscription): T =
            serialisedPropertyValue?.let {
                marshaller.unmarshall(it, subscription.propertyType) as T
            } ?: if (subscription.defaultValue.isPresent) {
                subscription.defaultValue.get() as T
            } else {
                throw DynamicPropertyNotFoundException(subscription.propertyName, subscription.propertyType)
            }

    override fun close() {
        Subscriptions.removeAllSubRef()
    }
}
