package ru.fix.dynamic.property.zk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.fix.dynamic.property.api.DynamicPropertySource;
import ru.fix.dynamic.property.jackson.JacksonDynamicPropertyMarshaller;
import ru.fix.dynamic.property.source.DefaultValue;
import ru.fix.dynamic.property.source.SourcedProperty;
import ru.fix.zookeeper.testing.ZKTestingServer;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;


public class ZkDynamicPropertySourceTest {

    private static final String TEST_PROP_KEY = "test_prop_key";
    private static final String TEST_PROP_KEY_1 = "test_prop_key_1";
    private static final String PROPERTIES_LOCATION = "/zookeeper/p";

    private static final Integer TIMEOUT_SEC = 10;

    private ZKTestingServer zkTestingServer;

    @BeforeEach
    public void beforeEach() throws Exception {
        zkTestingServer = new ZKTestingServer();
        zkTestingServer.start();
    }

    @Test
    public void shouldIgnoreDefaultValueIfPropertyExists() throws Exception {
        DynamicPropertySource source = createPropertySource();

        CountDownLatch propertyChanged = new CountDownLatch(1);
        AtomicReference<String> newPropertySlot = new AtomicReference<>();

        DynamicPropertySource.Subscription sub = source.subscribeAndCallListener(
                TEST_PROP_KEY,
                String.class,
                DefaultValue.of("zzz"),
                newValue -> {
                    newPropertySlot.set(newValue);
                    propertyChanged.countDown();
                });

        setServerProperty(PROPERTIES_LOCATION + "/" + TEST_PROP_KEY, "some Value");

        propertyChanged.await();
        assertEquals("some Value", newPropertySlot.get());
    }

    @Test
    public void shouldFetchActualValueOfProperty() throws Exception {
        DynamicPropertySource source = createPropertySource();

        setServerProperty(PROPERTIES_LOCATION + "/" + TEST_PROP_KEY, "some Value");

        CountDownLatch firstChange = new CountDownLatch(1);
        CountDownLatch secondChange = new CountDownLatch(1);

        AtomicReference<String> valueSlot = new AtomicReference<>();

        DynamicPropertySource.Subscription sub = source.subscribeAndCallListener(
                TEST_PROP_KEY,
                String.class,
                DefaultValue.none(),
                (value) -> {
                    valueSlot.set(value);
                    if (1 == firstChange.getCount()) {
                        firstChange.countDown();
                    } else {
                        secondChange.countDown();
                    }
                });

        firstChange.await();
        assertEquals("some Value", valueSlot.get());

        changeServerProperty(PROPERTIES_LOCATION + "/" + TEST_PROP_KEY, "some Value 2");

        secondChange.await();
        assertEquals("some Value 2", valueSlot.get());
    }

    @Test
    public void shouldListenChangeOfProperty() throws Exception {
        DynamicPropertySource source = createPropertySource();

        CountDownLatch propertyAdd = new CountDownLatch(1);
        CountDownLatch propertyChanged = new CountDownLatch(1);

        AtomicReference<String> valueSlot = new AtomicReference<>();

        DynamicPropertySource.Subscription sub = source.subscribeAndCallListener(
                TEST_PROP_KEY_1,
                String.class,
                DefaultValue.none(),
                value -> {
                    valueSlot.set(value);
                    if (1 == propertyAdd.getCount()) {
                        propertyAdd.countDown();
                    }

                    if (1 == propertyChanged.getCount()) {
                        propertyChanged.countDown();
                    }
                });

        setServerProperty(PROPERTIES_LOCATION + "/" + TEST_PROP_KEY_1, "some Value");
        propertyAdd.await();

        changeServerProperty(PROPERTIES_LOCATION + "/" + TEST_PROP_KEY_1, "some Value 2");
        assertTrue(propertyChanged.await(TIMEOUT_SEC, TimeUnit.SECONDS));

        assertEquals("some Value 2", valueSlot.get());
    }

    @Test
    public void shouldListenRemovingOfProperty() throws Exception {
        DynamicPropertySource source = createPropertySource();

        CountDownLatch propertyAdd = new CountDownLatch(1);
        CountDownLatch propertyRemoved = new CountDownLatch(1);
        DynamicPropertySource.Subscription sub = source.subscribeAndCallListener(
                TEST_PROP_KEY_1,
                String.class,
                DefaultValue.of("default"),
                value -> {
                    if ("default".equals(value)) {
                        propertyRemoved.countDown();
                    } else {
                        propertyAdd.countDown();
                    }
                });

        setServerProperty(PROPERTIES_LOCATION + "/" + TEST_PROP_KEY_1, "some Value");
        propertyAdd.await();

        removeServerProperty(PROPERTIES_LOCATION + "/" + TEST_PROP_KEY_1);
        assertTrue(propertyRemoved.await(TIMEOUT_SEC, TimeUnit.SECONDS));
    }

    @Test
    public void shouldFetchAllProperties() throws Exception {
        ZkDynamicPropertySource source = createPropertySource();

        setServerProperty(PROPERTIES_LOCATION + "/propName1", "some Value 1");
        setServerProperty(PROPERTIES_LOCATION + "/propName2", "some Value 2");

        Map<String, Object> childProperties = source.readAllProperties();
        assertNotNull(childProperties);
        assertEquals(2, childProperties.size());
        assertEquals("some Value 1", childProperties.get("propName1"));
        assertEquals("some Value 2", childProperties.get("propName2"));
    }

    @Test
    public void shouldNotFetchPropertiesIfNotPresent() throws Exception {
        ZkDynamicPropertySource source = createPropertySource();

        Map<String, Object> allProperties = source.readAllProperties();
        assertNotNull(allProperties);
        assertEquals(allProperties.size(), 0);
    }

    @Test
    public void shouldGetActualValueFromHolder() throws Exception {
        ZkDynamicPropertySource source = createPropertySource();

        String propertyNewValue = "some Value 2";

        CountDownLatch propertyAdded = new CountDownLatch(1);
        CountDownLatch propertyRemoved = new CountDownLatch(1);

        DynamicPropertySource.Subscription sub = source.subscribeAndCallListener(
                "propName1",
                String.class,
                DefaultValue.of("default"),
                value -> {
                    if (value.equals("default")) {
                        propertyRemoved.countDown();
                    } else {
                        propertyAdded.countDown();
                    }
                });

        SourcedProperty<String> propertyHolder = new SourcedProperty<>(
                source,
                "propName1",
                String.class,
                DefaultValue.none());

        retryAssert(null, propertyHolder::get);

        setServerProperty(PROPERTIES_LOCATION + "/propName1", propertyNewValue);
        propertyAdded.await();
        retryAssert(propertyNewValue, propertyHolder::get);

        removeServerProperty(PROPERTIES_LOCATION + "/propName1");
        propertyRemoved.await();
        retryAssert(null, propertyHolder::get);
    }

    public <T> void retryAssert(T expectedValue, Supplier<T> actualVale) throws InterruptedException {
        long assertEndTime = System.currentTimeMillis() + TIMEOUT_SEC;

        while (System.currentTimeMillis() <= assertEndTime) {
            if (Objects.equals(expectedValue, actualVale.get())) {
                break;
            }
            TimeUnit.MICROSECONDS.sleep(100);
        }

        assertEquals(expectedValue, actualVale.get());
    }

    @Test
    public void shouldGetDefaultValueFromHolder() throws Exception {
        ZkDynamicPropertySource source = createPropertySource();

        SourcedProperty<String> holder = new SourcedProperty<String>(
                source,
                "unknown.property",
                String.class,
                DefaultValue.of("default Value")
        );
        assertEquals("default Value", holder.get());
    }

    private ZkDynamicPropertySource createPropertySource() throws Exception {
        return new ZkDynamicPropertySource(
                zkTestingServer.getClient(),
                PROPERTIES_LOCATION,
                new JacksonDynamicPropertyMarshaller()
        );
    }

    private void setServerProperty(String propertyKey, String value) throws Exception {
        zkTestingServer.getClient().create().creatingParentsIfNeeded().forPath(
                propertyKey,
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void changeServerProperty(String propertyKey, String value) throws Exception {
        zkTestingServer.getClient().setData().forPath(propertyKey, value.getBytes(StandardCharsets.UTF_8));
    }

    private void removeServerProperty(String propertyKey) throws Exception {
        zkTestingServer.getClient().delete().deletingChildrenIfNeeded().forPath(propertyKey);
    }
}
