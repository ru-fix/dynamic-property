package ru.fix.dynamic.property.zk

import org.awaitility.Awaitility
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.fix.dynamic.property.api.source.OptionalDefaultValue
import ru.fix.dynamic.property.jackson.JacksonDynamicPropertyMarshaller
import ru.fix.zookeeper.testing.ZKTestingServer
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class ZkDynamicPropertySourceIT {
    companion object {
        private const val PROPERTIES_LOCATION = "/zookeeper/p"
    }

    private lateinit var zkTestingServer: ZKTestingServer

    @BeforeEach
    fun beforeEach() {
        zkTestingServer = ZKTestingServer().start()
    }

    @AfterEach
    fun afterEach() {
        zkTestingServer.close()
    }

    @Test
    fun `should return actual value instead of default when a large number of properties loaded`() {
        val generatedProperties = generateProperties(200)

        val source = ZkDynamicPropertySource(
                zkTestingServer.client,
                PROPERTIES_LOCATION,
                JacksonDynamicPropertyMarshaller(),
                Duration.of(1, ChronoUnit.MINUTES)
        )

        generatedProperties.forEach {
            setServerProperty("$PROPERTIES_LOCATION/${it.key}", it.value)
            source.subscribeAndCallListener(
                    it.key,
                    String::class.java,
                    OptionalDefaultValue.of("default")
            ) { value ->
                Assertions.assertNotEquals("default", value)
            }
        }
        source.close()
    }

    private fun setServerProperty(propertyKey: String, value: String) {
        val data = value.toByteArray(StandardCharsets.UTF_8)
        zkTestingServer.client
                .create()
                .creatingParentsIfNeeded()
                .forPath(propertyKey, data)

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
            zkTestingServer.client
                    .data
                    .forPath(propertyKey)!!.contentEquals(data)
        }
    }

    private fun generateProperties(count: Int): Map<String, String> {
        return (0..count).map { i -> Pair("prop-$i", "value-$i") }.toMap()
    }
}