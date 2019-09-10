package ru.fix.dynamic.property.spring

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.apache.logging.log4j.kotlin.Logging
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Scope
import ru.fix.dynamic.property.api.DynamicProperty
import ru.fix.dynamic.property.api.DynamicPropertySource
import ru.fix.dynamic.property.api.annotation.PropertyId
import ru.fix.dynamic.property.jackson.JacksonDynamicPropertyMarshaller
import ru.fix.dynamic.property.spring.config.DynamicPropertyConfig
import ru.fix.dynamic.property.std.source.InMemoryPropertySource

class CloseResourcesTest {
    companion object: Logging

    class ServiceConfig {
        init {
            logger.info("Create ServiceConfig")
        }

        @PropertyId("city")
        lateinit var city: DynamicProperty<String>
    }

    class Service {
        init {
            logger.info("Create Service")
        }

        @Autowired
        lateinit var config1: ServiceConfig

        @Autowired
        lateinit var config2: ServiceConfig
    }

    @Import(DynamicPropertyConfig::class)
    class Config{
        @Bean
        fun dynamicPropertySource(): DynamicPropertySource = InMemoryPropertySource(JacksonDynamicPropertyMarshaller())
                .apply {
                    this["city"] = "Kazan"
                }
        @Bean()
        @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
        fun serviceConfig() = ServiceConfig()

        @Bean
        fun service() = Service()
    }

    @Test
    fun `field container`() {
        val context = AnnotationConfigApplicationContext()
        context.register(Config::class.java)
        context.refresh()

        val config = context.getBean(ServiceConfig::class.java)
        context.config.

//        val service = context.getBean(Service::class.java)
//
//        for(config in listOf(service.config1, service.config2)) {
//            assertNotNull(config)
//            assertNotNull(config.city)
//            assertThat(config.city.get(), equalTo("Kazan"))
//        }

        context.close()
    }

}