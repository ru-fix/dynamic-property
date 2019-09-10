package ru.fix.dynamic.property.std.source

import ru.fix.dynamic.property.api.DynamicPropertyListener
import ru.fix.dynamic.property.api.DynamicPropertySource
import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller
import ru.fix.dynamic.property.source.DefaultValue
import ru.fix.dynamic.property.source.DynamicPropertyNotFoundException
import java.lang.ref.Cleaner
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import kotlin.collections.HashMap
import kotlin.collections.HashSet


class InMemoryPropertySource(private val marshaller: DynamicPropertyMarshaller) : DynamicPropertySource {

    private val properties = HashMap<String, String>()

    data class Subscription(
            val propertyName: String,
            val type: Class<Any>,
            val listener: DynamicPropertyListener<Any>) : DynamicPropertySource.Subscription{

        override fun close(){
//            TODO()
        }
    }


    private val propertySubscriptions = HashMap<String, MutableSet<Subscription>>()



    class SubscriptionWeakReference(referent: Subscription) : WeakReference<Subscription>(referent){
        val propertyName = referent.propertyName
    }

    val subscriptionReferenceQueue = ReferenceQueue<SubscriptionWeakReference>()

    init {
        while(true){
        val ref = subscriptionReferenceQueue.remove(250)

            }
        }
    }



    @Synchronized
    operator fun set(key: String, value: String) {
        properties[key] = value

        propertySubscriptions[key]?.forEach { sub ->
            sub.listener.onPropertyChanged(marshaller.unmarshall(value, sub.type))
        }
    }

    @Synchronized
    override fun <T> subscribeAndCallListener(
            propertyName: String,
            propertyType: Class<T>,
            defaultValue: DefaultValue<T>,
            listener: DynamicPropertyListener<T>): Subscription {

        val subscription = Subscription(
                propertyName,
                propertyType as Class<Any>,
                listener as DynamicPropertyListener<Any>)

        val queue = ReferenceQueue<Subscription>()
        val subRef = WeakReference<Subscription>(subscription, queue)


        Cleaner.create().register()



        propertySubscriptions
                .computeIfAbsent(propertyName) { key -> HashSet() }
                .add(subscription)

        val value = properties[propertyName]?.let {
            marshaller.unmarshall(it as String, propertyType)
        } ?: if (defaultValue.isPresent) {
            defaultValue.get()
        } else {
            throw DynamicPropertyNotFoundException(propertyName, propertyType)
        }
        listener.onPropertyChanged(value)

        return subscription
    }

    @Synchronized
    override fun unsubscribe(subscription: DynamicPropertySource.Subscription) {
        require(subscription is Subscription)
        propertySubscriptions[subscription.propertyName]?.let { subs ->
            subs.remove(subscription)
            if (subs.isEmpty()) {
                propertySubscriptions.remove(subscription.propertyName)
            }
        }
    }

    override fun close() {
        propertySubscriptions.clear()
    }
}
