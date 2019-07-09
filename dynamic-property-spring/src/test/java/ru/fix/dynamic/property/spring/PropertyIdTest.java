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
import ru.fix.dynamic.property.api.DynamicPropertySource;
import ru.fix.dynamic.property.jackson.JacksonDynamicPropertyMarshaller;
import ru.fix.dynamic.property.spring.config.DynamicPropertyConfig;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Ayrat Zulkarnyaev
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PropertyIdTest.Config.class)
public class PropertyIdTest {

    @Configuration
    @Import(DynamicPropertyConfig.class)
    @ComponentScan("ru.fix.dynamic.property.spring")
    public static class Config {

        @Bean
        public DynamicPropertySource dynamicPropertySource() {
            Properties properties = new Properties();
            properties.put("property.city", "Biribidjan");
            return new TestPropertySource(properties, new JacksonDynamicPropertyMarshaller());
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

}
