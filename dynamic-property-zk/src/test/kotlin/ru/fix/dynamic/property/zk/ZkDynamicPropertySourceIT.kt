package ru.fix.dynamic.property.zk

import org.awaitility.Awaitility
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
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
        private val log = LoggerFactory.getLogger(ZkDynamicPropertySource::class.java)
    }

    private lateinit var zkTestingServer: ZKTestingServer
    private lateinit var source: ZkDynamicPropertySource

    @BeforeEach
    fun beforeEach() {
        zkTestingServer = ZKTestingServer().start()
        source = ZkDynamicPropertySource(
                zkTestingServer.client,
                PROPERTIES_LOCATION,
                JacksonDynamicPropertyMarshaller(),
                Duration.of(1, ChronoUnit.MINUTES)
        )
    }

    @AfterEach
    fun afterEach() {
        source.close()
        zkTestingServer.close()
    }

    @Test
    fun `when loading a large number of properties`() {
        val generatedProperties = generateProperties(10000)

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
                    .forPath(propertyKey).contentEquals(data)
        }
    }

    private fun generateProperties(count: Int): Map<String, String> {
        val generatedProperties = HashMap<String, String>()

        for (i in 0..count) {
            generatedProperties["prop-$i"] = "value-$i"
        }

        return generatedProperties
    }
}