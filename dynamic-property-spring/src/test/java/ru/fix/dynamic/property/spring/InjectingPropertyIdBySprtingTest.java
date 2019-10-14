package ru.fix.dynamic.property.spring;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.fix.dynamic.property.api.DynamicProperty;
import ru.fix.dynamic.property.api.source.DynamicPropertySource;
import ru.fix.dynamic.property.api.annotation.PropertyId;
import ru.fix.dynamic.property.jackson.JacksonDynamicPropertyMarshaller;
import ru.fix.dynamic.property.spring.config.DynamicPropertyConfig;
import ru.fix.dynamic.property.std.source.InMemoryPropertySource;
import ru.fix.stdlib.concurrency.threads.ReferenceCleaner;

import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Ayrat Zulkarnyaev
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = InjectingPropertyIdBySprtingTest.Config.class)
public class InjectingPropertyIdBySprtingTest {

    public static class PropertyContainer {

        private DynamicProperty<String> status = DynamicProperty.of("NEW");

        @PropertyId("property.city")
        private DynamicProperty<String> city = DynamicProperty.of("kazan");

        public DynamicProperty<String> getCity() {
            return city;
        }
        public DynamicProperty<String> getStatus() {
            return status;
        }

        @PropertyId("nullable.property")
        private DynamicProperty<String> nullableProperty = DynamicProperty.of(null);
    }

    @Configuration
    @Import(DynamicPropertyConfig.class)
    @ComponentScan("ru.fix.dynamic.property.spring")
    public static class Config {

        @Bean
        public DynamicPropertySource dynamicPropertySource() {
            InMemoryPropertySource source = new InMemoryPropertySource(new JacksonDynamicPropertyMarshaller(), ReferenceCleaner.getInstance());
            source.set("property.city", "Biribidjan");
            return source;
        }

        @Bean
        public PropertyContainer propertyContainer() {
            return new PropertyContainer();
        }
    }

    @Autowired
    PropertyContainer propertyContainer;

    @Test
    public void addDynamicPropertyListener() {
        assertEquals("Biribidjan", propertyContainer.getCity().get());
    }

    @Test
    public void nullable_property_initialized_by_default_with_null(){
        assertNotNull(propertyContainer.nullableProperty);
        assertNull(propertyContainer.nullableProperty.get());
    }
}
