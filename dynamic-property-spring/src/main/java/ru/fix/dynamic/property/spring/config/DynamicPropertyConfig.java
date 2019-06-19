package ru.fix.dynamic.property.spring.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import ru.fix.dynamic.property.api.DynamicPropertySource;
import ru.fix.dynamic.property.spring.PropertyAwareBeanPostProcessor;
//import ru.fix.dynamic.config.api.DynamicPropertyMarshaller;
//import ru.fix.dynamic.config.api.DynamicPropertySource;
//import ru.fix.dynamic.config.spring.PropertyAwareBeanPostProcessor;
//import ru.fix.dynamic.config.spring.PropertySetAwareBeanProcessor;

/**
 * @author Ayrat Zulkarnyaev
 */
@Configuration
public class DynamicPropertyConfig {

    @Bean
//    @DependsOn("propertySetAwareBeanProcessor")
    @ConditionalOnClass(DynamicPropertySource.class)
    public PropertyAwareBeanPostProcessor propertyAwareBeanPostProcessor(DynamicPropertySource dynamicPropertySource) {
        return new PropertyAwareBeanPostProcessor(dynamicPropertySource);
    }

    //TODO: Решить, будем ли использовать
//    @Bean
//    @ConditionalOnClass({
//            DynamicPropertySource.class,
//            DynamicPropertyMarshaller.class
//    })
//    public PropertySetAwareBeanProcessor propertySetAwareBeanProcessor(DynamicPropertySource dynamicPropertySource,
//                                                                       DynamicPropertyMarshaller marshaller) {
//        return new PropertySetAwareBeanProcessor(dynamicPropertySource, marshaller);
//    }

}
