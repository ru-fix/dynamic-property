package ru.fix.dynamic.property.std.source

import org.slf4j.LoggerFactory
import ru.fix.dynamic.property.api.source.DynamicPropertySource
import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller
import ru.fix.dynamic.property.api.source.OptionalDefaultValue
import ru.fix.dynamic.property.api.source.DynamicPropertyValueNotFoundException
import ru.fix.stdlib.reference.CleanableWeakReference
import ru.fix.stdlib.reference.ReferenceCleaner
import java.lang.ref.Cleaner
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ConcurrentHashMap


abstract class AbstractPropertySource(
        private val marshaller: DynamicPropertyMarshaller,
        private val referenceCleaner: ReferenceCleaner = ReferenceCleaner.getInstance()) : DynamicPropertySource {

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractPropertySource::class.java)
    }

    private inner class Subscription(
            val propertyName: String,
            val propertyType: Class<Any>,
            val defaultValue: OptionalDefaultValue<*>,
            val listener: DynamicPropertySource.Listener<Any>) : DynamicPropertySource.Subscription {

        lateinit var cleanableReference: CleanableWeakReference<Subscription>

        override fun close() {
            //TODO: remove cleanableReference, search itself by direct unreferencing
            Subscriptions.removeSubRef(propertyName, cleanableReference)
        }
    }

    private object Subscriptions {
        val store = ConcurrentHashMap<String, MutableSet<CleanableWeakReference<Subscription>>>()

        fun addSubRef(propertyName: String, subRef: CleanableWeakReference<Subscription>) =
                store.compute(propertyName) { _, existingSet ->
                    val newSet = existingSet ?: Collections.newSetFromMap(ConcurrentHashMap())
                    newSet.add(subRef)
                    newSet
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

        fun removePrunedSubscriptionsAndGet(propertyName: String): MutableSet<CleanableWeakReference<Subscription>>? {
            return store.compute(propertyName) { _, set ->
                if (set != null) {
                    set.removeIf { ref ->
                        ref.get() == null
                    }
                    if (set.isEmpty()) {
                        null
                    } else {
                        set
                    }
                } else {
                    null
                }
            }
        }

        fun removeAllSubRef() {
            val propertyIter = store.iterator()
            while (propertyIter.hasNext()) {
                val subIter = propertyIter.next().value.iterator()
                while (subIter.hasNext()) {
                    subIter.next()
                    subIter.remove()
                }
                propertyIter.remove()
            }
        }
    }

    /**
     * @param newSerializedValue if null then property will be changed to default value
     *                           if default value is absent, then property does not receive an update
     */
    protected fun invokePropertyListener(propertyName: String, newSerializedValue: String?) {
        val subscriptoins = Subscriptions.removePrunedSubscriptionsAndGet(propertyName) ?: return

        subscriptoins.forEach { subRef ->
            subRef.get()?.let { sub ->
                try {
                    val newValue = extractPropertyValueOrDefault<Any?>(newSerializedValue, sub)
                    sub.listener.onPropertyChanged(newValue)
                } catch (exc: Exception) {
                    logger.error("Failed to update property {}", propertyName, exc)
                }
            }
        }
    }

    /**
     * @return null if there are no such property in PropertySource
     */
    protected abstract fun getPropertyValue(propertyName: String): String?

    override fun <T: Any?> subscribeAndCall(
            propertyName: String,
            propertyType: Class<T>,
            defaultValue: OptionalDefaultValue<T>,
            listener: DynamicPropertySource.Listener<T>): DynamicPropertySource.Subscription {

        val subscription = Subscription(
                propertyName,
                propertyType as Class<Any>,
                defaultValue,
                listener as DynamicPropertySource.Listener<Any>)
//TODO: weak reference test
        val subRef = referenceCleaner.register(subscription, propertyName) { a_ref, a_propName ->
            Subscriptions.removeSubRef(a_propName, a_ref)
        }

        //TODO remove clenableReference
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
                throw DynamicPropertyValueNotFoundException(subscription.propertyName, subscription.propertyType)
            }

    override fun close() {
        Subscriptions.removeAllSubRef()
    }
}
