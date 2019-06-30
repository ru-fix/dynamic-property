package ru.fix.dynamic.property.zk;

import org.apache.zookeeper.data.Stat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.dynamic.property.jackson.JacksonDynamicPropertyMarshaller;
import ru.fix.dynamic.property.source.DynamicSourceProperty;
import ru.fix.dynamic.property.api.DynamicPropertySource;
import ru.fix.dynamic.property.zk.test.ZKTestingServer;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;


public class ZkDynamicPropertySourceTest {

    private static final Logger log = LoggerFactory.getLogger(ZkDynamicPropertySourceTest.class);

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
        DynamicPropertySource zkConfig = createPropertySource(PROPERTIES_LOCATION);

        CountDownLatch propertyChanged = new CountDownLatch(1);
        zkConfig.addPropertyChangeListener(TEST_PROP_KEY, String.class, (newValue) -> propertyChanged.countDown());

        setServerProperty(PROPERTIES_LOCATION + "/" + TEST_PROP_KEY, "some Value");

        propertyChanged.await();
        assertEquals("some Value", zkConfig.getProperty(TEST_PROP_KEY, String.class));

        String property = zkConfig.getProperty(TEST_PROP_KEY, String.class);
        assertEquals("some Value", property);
    }

    @Test
    public void shouldFetchActualValueOfProperty() throws Exception {
        DynamicPropertySource zkConfig = createPropertySource(PROPERTIES_LOCATION);

        setServerProperty(PROPERTIES_LOCATION + "/" + TEST_PROP_KEY, "some Value");

        CountDownLatch firstChange = new CountDownLatch(1);
        CountDownLatch secondChange = new CountDownLatch(1);
        zkConfig.addPropertyChangeListener(TEST_PROP_KEY, String.class, (value) -> {
            if (1 == firstChange.getCount()) {
                firstChange.countDown();
            } else {
                secondChange.countDown();
            }
        });

        firstChange.await();
        assertEquals("some Value", zkConfig.getProperty(TEST_PROP_KEY, String.class));

        changeServerProperty(PROPERTIES_LOCATION + "/" + TEST_PROP_KEY, "some Value 2");

        secondChange.await();
        assertEquals("some Value 2", zkConfig.getProperty(TEST_PROP_KEY, String.class));

        String property = zkConfig.getProperty(TEST_PROP_KEY, String.class);
        assertEquals("some Value 2", property);
    }

    @Test
    public void shouldListenChangeOfProperty() throws Exception {
        DynamicPropertySource zkConfig = createPropertySource(PROPERTIES_LOCATION);

        CountDownLatch propertyAdd = new CountDownLatch(1);
        CountDownLatch propertyChanged = new CountDownLatch(1);
        zkConfig.addPropertyChangeListener(TEST_PROP_KEY_1, String.class, value -> {
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
    }

    @Test
    public void shouldListenRemovingOfProperty() throws Exception {
        DynamicPropertySource zkConfig = createPropertySource(PROPERTIES_LOCATION);

        CountDownLatch propertyAdd = new CountDownLatch(1);
        CountDownLatch propertyRemoved = new CountDownLatch(1);
        zkConfig.addPropertyChangeListener(TEST_PROP_KEY_1, String.class, value -> {
            if (null == value) {
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
    public void shouldCallCorrectListener() throws Exception {
        DynamicPropertySource zkConfig = createPropertySource(PROPERTIES_LOCATION);

        setServerProperty(PROPERTIES_LOCATION + "/" + TEST_PROP_KEY, "some Value 1");
        setServerProperty(PROPERTIES_LOCATION + "/" + TEST_PROP_KEY_1, "some Value 2");

        zkConfig.addPropertyChangeListener(TEST_PROP_KEY, String.class, value -> assertEquals("new some Value 1", value));

        zkConfig.addPropertyChangeListener(TEST_PROP_KEY_1, String.class, value -> assertEquals("new some Value 2", value));

        changeServerProperty(PROPERTIES_LOCATION + "/" + TEST_PROP_KEY, "new some Value 1");
        changeServerProperty(PROPERTIES_LOCATION + "/" + TEST_PROP_KEY_1, "new some Value 2");
    }

    @Test
    public void shouldFetchAllProperties() throws Exception {
        DynamicPropertySource zkConfig = createPropertySource(PROPERTIES_LOCATION);

        setServerProperty(PROPERTIES_LOCATION + "/propName1", "some Value 1");
        setServerProperty(PROPERTIES_LOCATION + "/propName2", "some Value 2");

        Properties childProperties = zkConfig.getAllProperties();
        assertNotNull(childProperties);
        assertEquals(2, childProperties.size());
        assertEquals("some Value 1", childProperties.getProperty("propName1"));
        assertEquals("some Value 2", childProperties.getProperty("propName2"));
    }

    @Test
    public void shouldFetchAllPropertiesIgnoringDefault() throws Exception {
        DynamicPropertySource zkConfig = createPropertySource(PROPERTIES_LOCATION);
        zkConfig.uploadInitialProperties("default.test.properties");

        updateServerProperty(PROPERTIES_LOCATION + "/propName1", "some Value 1");

        Properties allProperties = zkConfig.getAllProperties();
        assertNotNull(allProperties);
        assertEquals(allProperties.size(), 2);
        assertEquals("some Value 1", allProperties.getProperty("propName1"));
        assertEquals("propName2 default", allProperties.getProperty("propName2"));
    }

    @Test
    public void shouldCreateNotExistedDefaultProperty() throws Exception {
        DynamicPropertySource zkConfig = createPropertySource(PROPERTIES_LOCATION);

        String property = getServerProperty(PROPERTIES_LOCATION + "/propName1");
        assertNull(property);

        zkConfig.uploadInitialProperties("default.test.properties");

        property = getServerProperty(PROPERTIES_LOCATION + "/propName1");

        assertNotNull(property);
        assertEquals("propName1 default", property);
    }

    @Test
    public void shouldFetchEmptyPropertySetIfConfigLocationDoesNotExists() throws Exception {
        DynamicPropertySource zkConfig = createPropertySource(PROPERTIES_LOCATION + "/not-existed");
        zkConfig.uploadInitialProperties("default.test.properties");

        Properties allProperties = zkConfig.getAllProperties();

        assertNotNull(allProperties);
        assertEquals(2, allProperties.size());
        assertEquals("propName1 default", allProperties.getProperty("propName1"));
        assertEquals("propName2 default", allProperties.getProperty("propName2"));
    }

    @Test
    public void shouldGetActualValueFromHolder() throws Exception {
        ZkDynamicPropertySource zkConfig = createPropertySource(PROPERTIES_LOCATION);
        zkConfig.uploadInitialProperties("default.test.properties");

        String propertyNewValue = "some Value 2";

        DynamicSourceProperty<String> propertyHolder = new DynamicSourceProperty<>(zkConfig, "propName1", String.class);
        retryAssert("propName1 default", propertyHolder::get);

        updateServerProperty(PROPERTIES_LOCATION + "/propName1", propertyNewValue);
        retryAssert(propertyNewValue, propertyHolder::get);

        removeServerProperty(PROPERTIES_LOCATION + "/propName1");
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
        ZkDynamicPropertySource zkConfig = createPropertySource(PROPERTIES_LOCATION);

        DynamicSourceProperty<String> holder1 = new DynamicSourceProperty<>(
                zkConfig, "unknown.property", String.class, "default Value"
        );
        assertEquals("default Value", holder1.get());
    }

    private ZkDynamicPropertySource createPropertySource(String propertyLocation) throws Exception {
        return new ZkDynamicPropertySource(zkTestingServer.getClient(), propertyLocation, new JacksonDynamicPropertyMarshaller());
    }

    private void setServerProperty(String propertyKey, String value) throws Exception {
        zkTestingServer.getClient().create().creatingParentsIfNeeded().forPath(
                propertyKey,
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void updateServerProperty(String propertyKey, String value) throws Exception {
        zkTestingServer.getClient().setData().forPath(propertyKey, value.getBytes(StandardCharsets.UTF_8));
    }

    private void changeServerProperty(String propertyKey, String value) throws Exception {
        zkTestingServer.getClient().setData().forPath(propertyKey, value.getBytes(StandardCharsets.UTF_8));
    }

    private void removeServerProperty(String propertyKey) throws Exception {
        zkTestingServer.getClient().delete().deletingChildrenIfNeeded().forPath(propertyKey);
    }

    private String getServerProperty(String propertyKey) {
        try {
            Stat stat = zkTestingServer.getClient().checkExists().forPath(propertyKey);
            if (stat == null) {
                return null;
            }
            return new String(zkTestingServer.getClient().getData().forPath(propertyKey), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            return null;
        }
    }
}
