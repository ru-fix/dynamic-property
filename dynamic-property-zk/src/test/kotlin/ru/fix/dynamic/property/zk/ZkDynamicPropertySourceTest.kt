package ru.fix.dynamic.property.zk

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.fix.dynamic.property.jackson.JacksonDynamicPropertyMarshaller
import ru.fix.dynamic.property.source.DefaultValue
import ru.fix.dynamic.property.source.SourcedProperty
import ru.fix.zookeeper.testing.ZKTestingServer
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Supplier
import kotlin.collections.ArrayList
import kotlin.concurrent.withLock


class ZkDynamicPropertySourceTest {

    companion object {

        private val TEST_PROP_KEY = "test_prop_key"
        private val TEST_PROP_KEY_1 = "test_prop_key_1"
        private val PROPERTIES_LOCATION = "/zookeeper/p"
        private val TIMEOUT_SEC = 10


        lateinit var zkTestingServer: ZKTestingServer

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            zkTestingServer = ZKTestingServer()
            zkTestingServer.start()
        }
    }

    private lateinit var source: ZkDynamicPropertySource

    @BeforeEach
    fun beforeEach() {
        source = ZkDynamicPropertySource(
                zkTestingServer.client,
                PROPERTIES_LOCATION,
                JacksonDynamicPropertyMarshaller(),
                Duration.of(1, ChronoUnit.MINUTES)
        )
    }

    @AfterEach
    internal fun afterEach() {
        source.close()
    }

    @Test
    fun shouldIgnoreDefaultValueIfPropertyExists() {
        val propertyChanged = Semaphore(0)
        val newPropertySlot = AtomicReference<String>()

        val sub = source.subscribeAndCallListener(
                TEST_PROP_KEY,
                String::class.java,
                DefaultValue.of("zzz")
        ) { newValue ->
            newPropertySlot.set(newValue)
            propertyChanged.release()
        }

        propertyChanged.drainPermits()

        setServerProperty("$PROPERTIES_LOCATION/$TEST_PROP_KEY", "some Value")

        propertyChanged.acquire()
        assertEquals("some Value", newPropertySlot.get())
    }

    @Test
    fun shouldFetchActualValueOfProperty() {
        setServerProperty("$PROPERTIES_LOCATION/$TEST_PROP_KEY", "some Value")

        val firstChange = CountDownLatch(1)
        val secondChange = CountDownLatch(1)

        val valueSlot = AtomicReference<String>()

        val sub = source!!.subscribeAndCallListener(
                TEST_PROP_KEY,
                String::class.java,
                DefaultValue.none()
        ) { value ->
            valueSlot.set(value)
            if (1L == firstChange.count) {
                firstChange.countDown()
            } else {
                secondChange.countDown()
            }
        }

        firstChange.await()
        assertEquals("some Value", valueSlot.get())

        changeServerProperty("$PROPERTIES_LOCATION/$TEST_PROP_KEY", "some Value 2")

        secondChange.await()
        assertEquals("some Value 2", valueSlot.get())
    }


    @Test
    fun shouldListenChangeOfProperty() {
        val valueSlot = LinkedBlockingDeque<String>()

        val sub = source.subscribeAndCallListener(
                TEST_PROP_KEY_1,
                String::class.java,
                DefaultValue.of("zzz")
        ) { value ->
            valueSlot.add(value)
        }

        assertEquals("zzz", valueSlot.takeFirst())
        assertTrue(valueSlot.isEmpty())

        setServerProperty("$PROPERTIES_LOCATION/$TEST_PROP_KEY_1", "some Value")
        assertEquals("some Value", valueSlot.takeFirst())
        assertTrue(valueSlot.isEmpty())

        changeServerProperty("$PROPERTIES_LOCATION/$TEST_PROP_KEY_1", "some Value 2")
        assertEquals("some Value 2", valueSlot.takeFirst())
        assertTrue(valueSlot.isEmpty())
    }

    @Test
    fun shouldListenRemovingOfProperty() {
        val propertyAdd = CountDownLatch(1)
        val propertyRemoved = CountDownLatch(1)
        val sub = source!!.subscribeAndCallListener(
                TEST_PROP_KEY_1,
                String::class.java,
                DefaultValue.of("default")
        ) { value ->
            if ("default" == value) {
                propertyRemoved.countDown()
            } else {
                propertyAdd.countDown()
            }
        }

        setServerProperty("$PROPERTIES_LOCATION/$TEST_PROP_KEY_1", "some Value")
        propertyAdd.await()

        removeServerProperty("$PROPERTIES_LOCATION/$TEST_PROP_KEY_1")
        assertTrue(propertyRemoved.await(TIMEOUT_SEC.toLong(), TimeUnit.SECONDS))
    }

    @Test
    fun shouldFetchAllProperties() {
        setServerProperty("$PROPERTIES_LOCATION/propName1", "some Value 1")
        setServerProperty("$PROPERTIES_LOCATION/propName2", "some Value 2")

        val childProperties = source!!.readAllProperties()
        assertNotNull(childProperties)
        assertEquals(2, childProperties.size)
        assertEquals("some Value 1", childProperties["propName1"])
        assertEquals("some Value 2", childProperties["propName2"])
    }

    @Test
    fun shouldNotFetchPropertiesIfNotPresent() {
        val allProperties = source.readAllProperties()
        assertNotNull(allProperties)
        assertEquals(allProperties.size, 0)
    }

    @Test
    fun shouldGetActualValueFromHolder() {
        val propertyNewValue = "some Value 2"

        val propertyAdded = CountDownLatch(1)
        val propertyRemoved = CountDownLatch(1)

        val sub = source.subscribeAndCallListener(
                "propName1",
                String::class.java,
                DefaultValue.of("default")
        ) { value ->
            if (value == "default") {
                propertyRemoved.countDown()
            } else {
                propertyAdded.countDown()
            }
        }

        val propertyHolder = SourcedProperty(
                source,
                "propName1",
                String::class.java,
                DefaultValue.none())

        retryAssert(null, Supplier { propertyHolder.get() })

        setServerProperty("$PROPERTIES_LOCATION/propName1", propertyNewValue)
        propertyAdded.await()
        retryAssert(propertyNewValue, Supplier { propertyHolder.get() })

        removeServerProperty("$PROPERTIES_LOCATION/propName1")
        propertyRemoved.await()
        retryAssert(null, Supplier { propertyHolder.get() })
    }

    fun <T> retryAssert(expectedValue: T?, actualVale: Supplier<T>) {
        val assertEndTime = System.currentTimeMillis() + TIMEOUT_SEC

        while (System.currentTimeMillis() <= assertEndTime) {
            if (expectedValue == actualVale.get()) {
                break
            }
            TimeUnit.MICROSECONDS.sleep(100)
        }

        assertEquals(expectedValue, actualVale.get())
    }

    @Test
    fun shouldGetDefaultValueFromHolder() {
        val holder = SourcedProperty(
                source!!,
                "unknown.property",
                String::class.java,
                DefaultValue.of("default Value")
        )
        assertEquals("default Value", holder.get())
    }


    private fun setServerProperty(propertyKey: String, value: String) {
        zkTestingServer!!.client.create().creatingParentsIfNeeded().forPath(
                propertyKey,
                value.toByteArray(StandardCharsets.UTF_8)
        )
    }


    private fun changeServerProperty(propertyKey: String, value: String) {
        zkTestingServer!!.client.setData().forPath(propertyKey, value.toByteArray(StandardCharsets.UTF_8))
    }


    private fun removeServerProperty(propertyKey: String) {
        zkTestingServer!!.client.delete().deletingChildrenIfNeeded().forPath(propertyKey)
    }


}

class AtomicSlot<T : Any?>(value: T? = null) {
    private val lock = ReentrantLock()
    private val changed = lock.newCondition()
    private var ref: T? = value
    private var version = 0

    fun set(value: T) = lock.withLock {
        ref = value
        version++
        changed.signalAll()
    }

    fun get() = lock.withLock { ref }

    fun reset() = lock.withLock {
        ref = null
        version++
    }

    fun awaitChange(): Unit = lock.withLock {
        val currentVersion = version
        while (currentVersion == version) {
            changed.await()
        }
    }

    fun awaitChange(time: Long, unit: TimeUnit): Boolean = lock.withLock {
        val currentVersion = version
        val startTime = System.currentTimeMillis()
        val timeoutMs = unit.toMillis(time)

        while (currentVersion == version) {
            val spentTime = System.currentTimeMillis() - startTime
            val leftToWait = timeoutMs - spentTime

            if (leftToWait <= 0) return@withLock false

            changed.await(leftToWait, TimeUnit.MILLISECONDS)
        }

        return@withLock true
    }


}