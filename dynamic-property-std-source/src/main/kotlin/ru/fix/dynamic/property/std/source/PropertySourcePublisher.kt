package ru.fix.dynamic.property.std.source

import org.slf4j.LoggerFactory
import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller
import ru.fix.dynamic.property.api.source.DynamicPropertySource
import ru.fix.dynamic.property.api.source.DynamicPropertyValueNotFoundException
import ru.fix.dynamic.property.api.source.OptionalDefaultValue
import ru.fix.stdlib.reference.CleanableWeakReference
import ru.fix.stdlib.reference.ReferenceCleaner
import java.util.*
import java.util.concurrent.ConcurrentHashMap

interface PropertySourceAccessor {

    /**
     * Locks property value
     * Runs accessor
     * Release property value
     * During the lock PropertySource must no send any notification about property or change property value
     */
    fun accessPropertyUnderLock(propertyName: String, accessor: (String?) -> Unit)
}

class PropertySourcePublisher(
    private val propertySourceAccessor: PropertySourceAccessor,
    private val marshaller: DynamicPropertyMarshaller,
    private val referenceCleaner: ReferenceCleaner = ReferenceCleaner.getInstance()
) : DynamicPropertySource {

    companion object {
        private val logger = LoggerFactory.getLogger(PropertySourcePublisher::class.java)
    }

    private inner class Subscription<T>(
        val propertySource: PropertySourcePublisher,
        val propertyName: String,
        val propertyType: Class<Any>,
        val defaultValue: OptionalDefaultValue<*>

    ) : DynamicPropertySource.Subscription<T> {

        var listener: DynamicPropertySource.Listener<T>? = null
        var cleanableReference: CleanableWeakReference<Subscription<T>>? = null

        override fun setAndCallListener(
            listener: DynamicPropertySource.Listener<T>
        ): DynamicPropertySource.Subscription<*> {
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

        fun removePrunedSubscriptionsAndGet(
            propertyName: String
        ): MutableSet<CleanableWeakReference<Subscription<Any?>>>? {
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
     * Method should be invoked under property lock
     * in order to be consistent with [PropertySourceAccessor.accessPropertyUnderLock]
     *
     * @param newSerializedValue if null then property will be changed to default value
     *                           if default value is absent, then property does not receive an update
     */
    fun notifyAboutPropertyChange(propertyName: String, newSerializedValue: String?) {
        val subscriptoins = SubscriptionsRegistry.removePrunedSubscriptionsAndGet(propertyName) ?: return

        subscriptoins.forEach { subRef ->
            subRef.get()?.let { sub ->
                try {
                    val newValue = extractPropertyValueOrDefault(newSerializedValue, sub)
                    sub.listener!!.onPropertyChanged(newValue)
                } catch (exc: Exception) {
                    logger.error("Failed to update property {}", propertyName, exc)
                }
            }
        }
    }

    private fun <T> attachSubscriptionAndCallListener(subscription: Subscription<T>) {
        detachSubscription(subscription)

        //TODO: weak reference test
        val subRef = referenceCleaner.register(
            subscription, subscription.propertyName
        ) { a_ref, a_propName ->
            SubscriptionsRegistry.removeSubRef(
                a_propName,
                a_ref as CleanableWeakReference<Subscription<Any?>>
            )
        }

        subscription.cleanableReference = subRef

        propertySourceAccessor.accessPropertyUnderLock(subscription.propertyName) { propertyValue ->
            SubscriptionsRegistry.addSubRef(
                subscription.propertyName,
                subRef as CleanableWeakReference<Subscription<Any?>>
            )

            val value = extractPropertyValueOrDefault(propertyValue, subscription)

            subscription.listener!!.onPropertyChanged(value)
        }
    }

    private fun <T : Any?> detachSubscription(subscription: Subscription<T>) {
        if (subscription.cleanableReference != null) {
            SubscriptionsRegistry.removeSubRef(
                subscription.propertyName,
                subscription.cleanableReference!! as CleanableWeakReference<Subscription<Any?>>
            )
            subscription.cleanableReference = null
        }
    }

    override fun <T : Any?> createSubscription(
        propertyName: String,
        propertyType: Class<T>,
        defaultValue: OptionalDefaultValue<T>
    ): DynamicPropertySource.Subscription<T> {

        return Subscription(
            this,
            propertyName,
            propertyType as Class<Any>,
            defaultValue
        )
    }

    private fun <T : Any?> extractPropertyValueOrDefault(
        serialisedPropertyValue: String?,
        subscription: Subscription<T>
    ): T = serialisedPropertyValue?.let {
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
