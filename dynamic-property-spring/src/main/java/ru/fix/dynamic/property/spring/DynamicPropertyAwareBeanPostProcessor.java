package ru.fix.dynamic.property.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;
import ru.fix.dynamic.property.source.SourcedProperty;
import ru.fix.dynamic.property.api.DynamicProperty;
import ru.fix.dynamic.property.api.DynamicPropertySource;
import ru.fix.dynamic.property.api.annotation.PropertyId;
import ru.fix.dynamic.property.spring.exception.DynamicPropertyDefaultValueNotDefinedException;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author Kamil Asfandiyarov
 */
public class DynamicPropertyAwareBeanPostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DynamicPropertyAwareBeanPostProcessor.class);

    private DynamicPropertySource propertySource;
    private LongAdder processingTime = new LongAdder();

    public DynamicPropertyAwareBeanPostProcessor(DynamicPropertySource propertySource) {
        this.propertySource = propertySource;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        final long startTime = System.currentTimeMillis();

        ReflectionUtils.doWithFields(bean.getClass(), field -> {

            field.setAccessible(true);

            PropertyId propertyIdAnnotation = field.getAnnotation(PropertyId.class);
            if (Objects.nonNull(propertyIdAnnotation)) {
                field.set(bean, processDynamicProperty(bean, field, propertyIdAnnotation, beanName));
            }
        });

        final long currentProcessingTime = System.currentTimeMillis() - startTime;
        processingTime.add(currentProcessingTime);
        log.debug("Resolving @PropertyId annotation for '{}' bean took {} ms. " +
                        "Sum of processing times is equal {} ms now.",
                beanName, currentProcessingTime, processingTime.sum()
        );

        return bean;
    }

    private Object processDynamicProperty(Object bean, Field field, PropertyId propertyIdAnnotation, String beanName) {

        Class<?> fieldType = field.getType();

        if (fieldType.isAssignableFrom(DynamicProperty.class)) {

            String propertyId = propertyIdAnnotation.value();

            Class propertyClass = extractPropertyClass(field);
            Object propertyDefaultValue = extractDefaultValue(bean, field);
            if (null == propertyDefaultValue) {
                String errorMessage = String.format(
                        "Illegal default property value '%s' of bean '%s'. " +
                                "DynamicProperty type annotated by @PropertyId must have default value other than null",
                        field.getName(), beanName
                );
                throw new DynamicPropertyDefaultValueNotDefinedException(errorMessage);
            }
            //noinspection unchecked
            return new SourcedProperty<>(propertySource, propertyId, propertyClass, propertyDefaultValue);

        } else {
            log.warn(
                    "@PropertyId annotation is applicable only on fields " +
                            "of DynamicProperty type, not '{}', bean '{}'",
                    fieldType, beanName
            );
            return null;
        }
    }

    private Class extractPropertyClass(Field field) {
        ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
        Type type = parameterizedType.getActualTypeArguments()[0];
        Class propertyClass;
        if (type instanceof ParameterizedType) {
            propertyClass = (Class) ((ParameterizedType) type).getRawType();
        } else {
            propertyClass = (Class) type;
        }
        return propertyClass;
    }

    private Object extractDefaultValue(Object bean, Field field) {
        DynamicProperty<?> dynamicProperty = null;
        try {
            dynamicProperty = (DynamicProperty<?>) field.get(bean);
        } catch (IllegalAccessException e) {
            log.error("Error occurred when extracting value from field '{}'", field.getName());
        }
        return Optional.ofNullable(dynamicProperty)
                .map(DynamicProperty::get)
                .orElse(null);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
