package ru.fix.dynamic.property.std.source

import org.slf4j.LoggerFactory
import ru.fix.dynamic.property.api.source.DynamicPropertySource
import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller
import ru.fix.dynamic.property.api.source.OptionalDefaultValue
import ru.fix.dynamic.property.api.source.DynamicPropertyValueNotFoundException
import ru.fix.stdlib.reference.CleanableWeakReference
import ru.fix.stdlib.reference.ReferenceCleaner
import java.util.*
import java.util.concurrent.ConcurrentHashMap


abstract class AbstractPropertySource(
        private val marshaller: DynamicPropertyMarshaller,
        private val referenceCleaner: ReferenceCleaner = ReferenceCleaner.getInstance()) : DynamicPropertySource {

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractPropertySource::class.java)
    }

    private inner class Subscription<T>(
            val propertySource: AbstractPropertySource,
            val propertyName: String,
            val propertyType: Class<Any>,
            val defaultValue: OptionalDefaultValue<*>

    ) : DynamicPropertySource.Subscription<T> {

        var listener: DynamicPropertySource.Listener<T>? = null
        var cleanableReference: CleanableWeakReference<Subscription<T>>? = null

        override fun setAndCallListener(listener: DynamicPropertySource.Listener<T>): DynamicPropertySource.Subscription<*> {
            this.listener = listener
            propertySource.attachSubscriptionAndCallListener(this)
            return this
        }

        override fun close() {
            propertySource.detachSubscription(this)
        }
    }

    private object SubscriptionsRegistry {
        val store = ConcurrentHashMap<String, MutableSet<CleanableWeakReference<Subscription<Any?>>>>()

        fun addSubRef(propertyName: String, subRef: CleanableWeakReference<Subscription<Any?>>) =
                store.compute(propertyName) { _, existingSet ->
                    val newSet = existingSet ?: Collections.newSetFromMap(ConcurrentHashMap())
                    newSet.add(subRef)
                    newSet
                }

        fun removeSubRef(propertyName: String, subRef: CleanableWeakReference<Subscription<Any?>>) =
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

        fun removePrunedSubscriptionsAndGet(propertyName: String): MutableSet<CleanableWeakReference<Subscription<Any?>>>? {
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
    @Synchronized
    protected fun invokePropertyListener(propertyName: String, newSerializedValue: String?) {
        val subscriptoins = SubscriptionsRegistry.removePrunedSubscriptionsAndGet(propertyName) ?: return

        subscriptoins.forEach { subRef ->
            subRef.get()?.let { sub ->
                try {
                    val newValue = extractPropertyValueOrDefault<Any?>(newSerializedValue, sub)
                    sub.listener!!.onPropertyChanged(newValue)
                } catch (exc: Exception) {
                    logger.error("Failed to update property {}", propertyName, exc)
                }
            }
        }
    }

    //TODO: Synchronized all required things

    /**
     * @return null if there are no such property in PropertySource
     */
    protected abstract fun getPropertyValue(propertyName: String): String?

    @Synchronized
    private fun <T> attachSubscriptionAndCallListener(subscription: Subscription<T>) {
        detachSubscription(subscription)

        //TODO: weak reference test
        val subRef = referenceCleaner.register(
                subscription, subscription.propertyName) { a_ref, a_propName ->
            SubscriptionsRegistry.removeSubRef(
                    a_propName,
                    a_ref as CleanableWeakReference<Subscription<Any?>>)
        }

        subscription.cleanableReference = subRef
        SubscriptionsRegistry.addSubRef(
                subscription.propertyName,
                subRef as CleanableWeakReference<Subscription<Any?>>)

        val value = extractPropertyValueOrDefault(getPropertyValue(subscription.propertyName), subscription)
        subscription.listener!!.onPropertyChanged(value)
    }

    private fun <T: Any?> detachSubscription(subscription: Subscription<T>) {
        if (subscription.cleanableReference != null) {
            SubscriptionsRegistry.removeSubRef(
                    subscription.propertyName,
                    subscription.cleanableReference!! as CleanableWeakReference<Subscription<Any?>>)
            subscription.cleanableReference = null
        }
    }

    override fun <T : Any?> createSubscription(
            propertyName: String,
            propertyType: Class<T>,
            defaultValue: OptionalDefaultValue<T>): DynamicPropertySource.Subscription<T> {

        return Subscription(
                this,
                propertyName,
                propertyType as Class<Any>,
                defaultValue)
    }

    private fun <T : Any?> extractPropertyValueOrDefault(serialisedPropertyValue: String?, subscription: Subscription<T>): T =
            serialisedPropertyValue?.let {
                marshaller.unmarshall(it, subscription.propertyType) as T
            } ?: if (subscription.defaultValue.isPresent) {
                subscription.defaultValue.get() as T
            } else {
                throw DynamicPropertyValueNotFoundException(subscription.propertyName, subscription.propertyType)
            }

    override fun close() {
        SubscriptionsRegistry.removeAllSubRef()
    }
}
