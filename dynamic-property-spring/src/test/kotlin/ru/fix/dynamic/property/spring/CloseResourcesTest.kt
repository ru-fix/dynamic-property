package ru.fix.dynamic.property.spring

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.apache.logging.log4j.kotlin.Logging
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import ru.fix.dynamic.property.api.DynamicProperty
import ru.fix.dynamic.property.api.DynamicPropertyListener
import ru.fix.dynamic.property.api.source.DynamicPropertySource
import ru.fix.dynamic.property.api.annotation.PropertyId
import ru.fix.dynamic.property.jackson.JacksonDynamicPropertyMarshaller
import ru.fix.dynamic.property.api.source.OptionalDefaultValue
import ru.fix.dynamic.property.spring.config.DynamicPropertyConfig
import ru.fix.dynamic.property.std.source.InMemoryPropertySource
import java.util.concurrent.atomic.AtomicInteger

class CloseResourcesTest {
    companion object: Logging

    class ServiceConfig {
        init {
            logger.info("Create ServiceConfig")
        }

        @PropertyId("city")
        lateinit var city: DynamicProperty<String>

        @Autowired
        lateinit var resource: ClosableResource
    }

    class ClosableResource : AutoCloseable{
        companion object{
            val closedResourceCount = AtomicInteger()
        }

        override fun close() {
            closedResourceCount.incrementAndGet()
        }
    }

    @Import(DynamicPropertyConfig::class)
    class Config{
        @Bean
        fun dynamicPropertySource(): DynamicPropertySource {
            return MockedPropertySource.apply {
                this["city"] = "Kazan"
            }
        }

        @Bean()
        fun closableResource() = ClosableResource()

        @Bean()
        fun serviceConfig() = ServiceConfig()
    }

    object MockedPropertySource: InMemoryPropertySource(JacksonDynamicPropertyMarshaller()){
        val closedSubscriptions = AtomicInteger()

        override fun <T> subscribeAndCallListener(
                propertyName: String,
                propertyType: Class<T>,
                defaultValue: OptionalDefaultValue<T>,
                listener: DynamicPropertySource.Listener<T>): DynamicPropertySource.Subscription {

            val subscription =  super.subscribeAndCallListener(propertyName, propertyType, defaultValue, listener)

            return DynamicPropertySource.Subscription {
                closedSubscriptions.incrementAndGet()
                subscription.close()
            }
        }
    }

    @Test
    fun `spring close injected dynamic property instances`() {
        val context = AnnotationConfigApplicationContext()
        context.register(Config::class.java)
        context.refresh()

        var serviceConfig = context.getBean(ServiceConfig::class.java)
        with(serviceConfig) {
            assertNotNull(city)
            assertThat(city.get(), equalTo("Kazan"))
        }

        assertThat(ClosableResource.closedResourceCount.get(), equalTo(0))
        assertThat(MockedPropertySource.closedSubscriptions.get(), equalTo(0))

        context.close()

        assertThat(ClosableResource.closedResourceCount.get(), equalTo(1))
        assertThat(MockedPropertySource.closedSubscriptions.get(), equalTo(1))
    }

}