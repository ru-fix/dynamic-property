package ru.fix.dynamic.property.spring.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.fix.dynamic.property.api.DynamicPropertySource;
import ru.fix.dynamic.property.spring.DynamicPropertyAwareBeanPostProcessor;

/**
 * @author Ayrat Zulkarnyaev
 */
@Configuration
public class DynamicPropertyConfig {

    @Bean
    @ConditionalOnClass(DynamicPropertySource.class)
    public DynamicPropertyAwareBeanPostProcessor propertyAwareBeanPostProcessor(DynamicPropertySource dynamicPropertySource) {
        return new DynamicPropertyAwareBeanPostProcessor(dynamicPropertySource);
    }
}
