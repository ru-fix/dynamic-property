package ru.fix.dynamic.property.spring;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.fix.dynamic.property.api.DynamicProperty;
import ru.fix.dynamic.property.api.PropertySubscription;
import ru.fix.dynamic.property.api.annotation.PropertyId;
import ru.fix.dynamic.property.api.source.DynamicPropertySource;
import ru.fix.dynamic.property.api.source.DynamicPropertyValueNotFoundException;
import ru.fix.dynamic.property.jackson.JacksonDynamicPropertyMarshaller;
import ru.fix.dynamic.property.std.source.InMemoryPropertySource;
import ru.fix.stdlib.reference.ReferenceCleaner;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

class DynamicPropertyAwareBeanPostProcessorTest {

    InMemoryPropertySource propertySource;
    DynamicPropertyAwareBeanPostProcessor processor;

    @BeforeEach
    void createSourceAndProcessor() {
        propertySource = new InMemoryPropertySource(
                new JacksonDynamicPropertyMarshaller(),
                ReferenceCleaner.getInstance()
        );
        processor = new DynamicPropertyAwareBeanPostProcessor(propertySource);
    }

    @AfterEach
    void closeSource() {
        propertySource.close();
    }

    public static class PropertyContainer {

        private final DynamicProperty<String> status = DynamicProperty.of("NEW");

        @PropertyId("property.city")
        private final DynamicProperty<String> city = DynamicProperty.of("kazan");

        public DynamicProperty<String> getCity() {
            return city;
        }

        public DynamicProperty<String> getStatus() {
            return status;
        }

        @PropertyId("property.temperature")
        private final PropertySubscription<Integer> temperature = PropertySubscription.of(11);

    }

    @Test
    public void processBean_shouldInitializeAllDynamicPropertiesByDefault() {
        PropertyContainer bean = new PropertyContainer();

        PropertyContainer processedBean =
                (PropertyContainer) processor.postProcessBeforeInitialization(bean, "propertyContainer");

        assertNotNull(processedBean.getCity().get());
        assertEquals(processedBean.getCity().get(), "kazan");
    }

    @Test
    public void processBean_shouldInitializeAllDynamicPropertiesFromSource() {
        propertySource.set("property.city", "Moscow");

        PropertyContainer bean = new PropertyContainer();

        PropertyContainer processedBean =
                (PropertyContainer) processor.postProcessBeforeInitialization(bean, "propertyContainer");

        assertEquals("Moscow", processedBean.getCity().get());
        assertEquals("NEW", processedBean.getStatus().get(), "Must initialize annotated by @PropertyId");
    }


    @Test
    public void processBeanWithoutProperty() {
        processor.postProcessBeforeInitialization(new Object(), "withoutProperty");
    }

    static class PropertyWithoutDefaultValue {
        @PropertyId("property.city")
        private DynamicProperty<String> city;

        public DynamicProperty<String> getCity() {
            return city;
        }
    }

    @Test
    public void propertyWithoutDefault_withoutSource_mustCompleteExceptionally() {
        PropertyWithoutDefaultValue bean = new PropertyWithoutDefaultValue();

        assertThrows(DynamicPropertyValueNotFoundException.class, () ->
                processor.postProcessBeforeInitialization(bean, "propertyWithoutDefaultValue")
        );
    }

    @Test
    public void propertyWithoutDefault_withSource_mustUseValueFromSource() {
        propertySource.set("property.city", "Moscow");

        PropertyWithoutDefaultValue bean = new PropertyWithoutDefaultValue();
        processor.postProcessBeforeInitialization(bean, "propertyWithoutDefaultValue");

        assertEquals("Moscow", bean.city.get());

    }

    @Test
    public void propertySubscription_uses_value_from_source_if_it_exist() {
        propertySource.set("property.temperature", "42");

        PropertyContainer bean = new PropertyContainer();
        processor.postProcessBeforeInitialization(bean, "propertyContainer");

        assertEquals(42, bean.temperature.get());
    }

    @Test
    public void propertySubscription_uses_default_value_from_code_if_source_is_empty() {
        PropertyContainer bean = new PropertyContainer();
        processor.postProcessBeforeInitialization(bean, "propertyContainer");

        assertEquals(11, bean.temperature.get());
    }

    @Test
    public void propertySubscription_calls_listener_when_source_updated() {
        propertySource.set("property.temperature", "42");

        PropertyContainer bean = new PropertyContainer();
        processor.postProcessBeforeInitialization(bean, "propertyContainer");

        assertEquals(42, bean.temperature.get());

        AtomicReference<Integer> newValueSlot = new AtomicReference<>();
        AtomicReference<Integer> oldValueSlot = new AtomicReference<>();
        bean.temperature.setAndCallListener((oldValue, newValue) -> {
            oldValueSlot.set(oldValue);
            newValueSlot.set(newValue);
        });
        propertySource.set("property.temperature", "45");
        await().atMost(1, MINUTES).until(
                () -> oldValueSlot.get() == 42 && newValueSlot.get() == 45
        );
    }

    public static class ListenerInConstructorContainer {

        @PropertyId("property.city")
        final DynamicProperty<String> city = DynamicProperty.of("kazan");
        PropertySubscription<String> citySubscription;

        final AtomicReference<String> citiListenerOldValueSlot = new AtomicReference<>();
        final AtomicReference<String> citiListenerNewValueSlot = new AtomicReference<>();

        @PropertyId("property.temperature")
        private final PropertySubscription<Integer> temperature = PropertySubscription.of(11);
        final AtomicReference<Integer> temperatureListenerOldValueSlot = new AtomicReference<>();
        final AtomicReference<Integer> temperatureListenerNewValueSlot = new AtomicReference<>();

        public void postConstructInitialization() {
            citySubscription = city.createSubscription()
                    .setAndCallListener((oldValue, newValue) -> {
                        citiListenerOldValueSlot.set(oldValue);
                        citiListenerNewValueSlot.set(newValue);
                    });

            temperature.setAndCallListener((oldValue, newValue) -> {
                temperatureListenerOldValueSlot.set(oldValue);
                temperatureListenerNewValueSlot.set(newValue);
            });
        }
    }

    @Test
    void listeners_registered_in_post_constuct_initialization_method() {
        ListenerInConstructorContainer bean = new ListenerInConstructorContainer();
        processor.postProcessBeforeInitialization(bean, "listenerInConstructorContainer");

        bean.postConstructInitialization();

        assertEquals("kazan", bean.city.get());
        assertNull(bean.citiListenerOldValueSlot.get());
        assertEquals("kazan", bean.citiListenerNewValueSlot.get());

        propertySource.set("property.city", "moscow");

        await().atMost(10, SECONDS).until(() -> bean.city.get().contentEquals("moscow"));

        await().atMost(10, SECONDS).until(() ->
                bean.citiListenerNewValueSlot.get() != null &&
                        bean.citiListenerNewValueSlot.get().contentEquals("moscow"));

        await().atMost(10, SECONDS).until(() ->
                bean.citiListenerOldValueSlot.get() != null &&
                        bean.citiListenerOldValueSlot.get().contentEquals("kazan")
        );


        assertEquals(11, bean.temperature.get());
        assertNull(bean.temperatureListenerOldValueSlot.get());
        assertEquals(11, bean.temperatureListenerNewValueSlot.get());

        propertySource.set("property.temperature", "70");

        await().atMost(10, SECONDS).until(() -> bean.temperature.get() == 70);
        await().atMost(10, SECONDS).until(() ->
                bean.temperatureListenerOldValueSlot.get() != null &&
                        bean.temperatureListenerOldValueSlot.get() == 11);
        await().atMost(10, SECONDS).until(() ->
                bean.temperatureListenerNewValueSlot.get() != null &&
                        bean.temperatureListenerNewValueSlot.get() == 70);
    }
}