package ru.fix.dynamic.property.spring;

import org.junit.jupiter.api.Test;
import ru.fix.dynamic.property.api.DynamicProperty;
import ru.fix.dynamic.property.api.annotation.PropertyId;
import ru.fix.dynamic.property.api.source.DynamicPropertyValueNotFoundException;
import ru.fix.dynamic.property.jackson.JacksonDynamicPropertyMarshaller;
import ru.fix.dynamic.property.std.source.InMemoryPropertySource;
import ru.fix.stdlib.reference.ReferenceCleaner;

import static org.junit.jupiter.api.Assertions.*;

class DynamicPropertyAwareBeanPostProcessorTest {

    InMemoryPropertySource propertySource;
    DynamicPropertyAwareBeanPostProcessor processor;

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
    }

    @Test
    public void processBean_shouldInitializeAllDynamicPropertiesByDefault() {
        propertySource = new InMemoryPropertySource(
                new JacksonDynamicPropertyMarshaller(),
                ReferenceCleaner.getInstance()
        );
        processor = new DynamicPropertyAwareBeanPostProcessor(propertySource);

        PropertyContainer bean = new PropertyContainer();

        PropertyContainer processedBean =
                (PropertyContainer) processor.postProcessBeforeInitialization(bean, "propertyContainer");

        assertNotNull(processedBean.getCity().get());
        assertEquals(processedBean.getCity().get(), "kazan");
    }

    @Test
    public void processBean_shouldInitializeAllDynamicPropertiesFromSource() {

        propertySource = new InMemoryPropertySource(
                new JacksonDynamicPropertyMarshaller(),
                ReferenceCleaner.getInstance()
        );
        propertySource.set("property.city", "Moscow");

        processor = new DynamicPropertyAwareBeanPostProcessor(propertySource);

        PropertyContainer bean = new PropertyContainer();

        PropertyContainer processedBean =
                (PropertyContainer) processor.postProcessBeforeInitialization(bean, "propertyContainer");

        assertEquals("Moscow", processedBean.getCity().get());
        assertEquals("NEW", processedBean.getStatus().get(), "Must initialize annotated by @PropertyId");
    }


    @Test
    public void processBeanWithoutProperty() {
        propertySource = new InMemoryPropertySource(
                new JacksonDynamicPropertyMarshaller(),
                ReferenceCleaner.getInstance()
        );
        processor = new DynamicPropertyAwareBeanPostProcessor(propertySource);

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
        propertySource = new InMemoryPropertySource(
                new JacksonDynamicPropertyMarshaller(),
                ReferenceCleaner.getInstance()
        );
        processor = new DynamicPropertyAwareBeanPostProcessor(propertySource);

        PropertyWithoutDefaultValue bean = new PropertyWithoutDefaultValue();

        assertThrows(DynamicPropertyValueNotFoundException.class, () ->
                processor.postProcessBeforeInitialization(bean, "propertyWithoutDefaultValue")
        );
    }

    @Test
    public void propertyWithoutDefault_withSource_mustUseValueFromSource() {
        propertySource = new InMemoryPropertySource(
                new JacksonDynamicPropertyMarshaller(),
                ReferenceCleaner.getInstance()
        );
        propertySource.set("property.city", "Moscow");
        processor = new DynamicPropertyAwareBeanPostProcessor(propertySource);

        PropertyWithoutDefaultValue bean = new PropertyWithoutDefaultValue();
        processor.postProcessBeforeInitialization(bean, "propertyWithoutDefaultValue");

        assertEquals("Moscow", bean.city.get());

    }
}