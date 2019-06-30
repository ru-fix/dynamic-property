package ru.fix.dynamic.property.spring;

import org.junit.jupiter.api.Test;
import ru.fix.dynamic.property.api.DynamicProperty;
import ru.fix.dynamic.property.api.DynamicPropertySource;
import ru.fix.dynamic.property.api.annotation.PropertyId;
import ru.fix.dynamic.property.api.marshaller.DefaultDynamicPropertyMarshaller;
import ru.fix.dynamic.property.spring.exception.DynamicPropertyDefaultValueNotFoundException;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class DynamicPropertyAwareBeanPostProcessorTest {

    DynamicPropertySource propertySource;

    DynamicPropertyAwareBeanPostProcessor processor;

    @Test
    public void processBean_shouldInitializeAllDynamicPropertiesByDefault() {
        propertySource = new TestPropertySource(new Properties(), new DefaultDynamicPropertyMarshaller());
        processor = new DynamicPropertyAwareBeanPostProcessor(propertySource);

        PropertyContainer bean = new PropertyContainer();

        PropertyContainer processedBean = (PropertyContainer) processor.postProcessBeforeInitialization(bean, "propertyContainer");

        assertEquals("kazan", processedBean.getCity().get());
    }

    @Test
    public void processBean_shouldInitializeAllDynamicPropertiesFromSource() {
        Properties properties = new Properties();
        properties.put("property.city", "Moscow");
        propertySource = new TestPropertySource(properties, new DefaultDynamicPropertyMarshaller());
        processor = new DynamicPropertyAwareBeanPostProcessor(propertySource);

        PropertyContainer bean = new PropertyContainer();

        PropertyContainer processedBean = (PropertyContainer) processor.postProcessBeforeInitialization(bean, "propertyContainer");

        assertEquals("Moscow", processedBean.getCity().get());
        assertEquals( "NEW", processedBean.getStatus().get(), "Must initialize annotated by @PropertyId");
    }

    @Test
    public void processBean_propertyWithoutDefault_mustCompleteExceptionally() {
        propertySource = new TestPropertySource(new Properties(), new DefaultDynamicPropertyMarshaller());
        processor = new DynamicPropertyAwareBeanPostProcessor(propertySource);

        PropertyWithoutDefaultValue bean = new PropertyWithoutDefaultValue();

        assertThrows(DynamicPropertyDefaultValueNotFoundException.class, () ->
                processor.postProcessBeforeInitialization(bean, "propertyWithoutDefaultValue")
        );
    }

    @Test
    public void processBeanWithoutProperty() {
        propertySource = new TestPropertySource(new Properties(), new DefaultDynamicPropertyMarshaller());
        processor = new DynamicPropertyAwareBeanPostProcessor(propertySource);

        processor.postProcessBeforeInitialization(new WithoutProperty(), "withoutProperty");
    }

    class PropertyWithoutDefaultValue {
        @PropertyId("property.city")
        private DynamicProperty<String> city;

        public DynamicProperty<String> getCity() {
            return city;
        }
    }

    class WithoutProperty { }
}