package ru.fix.dynamic.property.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.util.ReflectionUtils;
import ru.fix.dynamic.property.api.DynamicProperty;
import ru.fix.dynamic.property.api.PropertySubscription;
import ru.fix.dynamic.property.api.annotation.PropertyId;
import ru.fix.dynamic.property.api.source.DynamicPropertySource;
import ru.fix.dynamic.property.api.source.DynamicPropertyValueNotFoundException;
import ru.fix.dynamic.property.api.source.OptionalDefaultValue;
import ru.fix.dynamic.property.api.source.SourcedProperty;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author Kamil Asfandiyarov
 */
public class DynamicPropertyAwareBeanPostProcessor implements DestructionAwareBeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DynamicPropertyAwareBeanPostProcessor.class);

    private final DynamicPropertySource propertySource;
    private final LongAdder processingTime = new LongAdder();

    private final Map<Object, Object> constructedObjects = Collections.synchronizedMap(new WeakHashMap<>());

    public DynamicPropertyAwareBeanPostProcessor(DynamicPropertySource propertySource) {
        this.propertySource = propertySource;
    }

    @FunctionalInterface
    private interface AnnotatedBeanProcessor {
        void process(Object bean, Field field, PropertyId annotation) throws IllegalAccessException, DynamicPropertyValueNotFoundException;
    }

    private void doWithAnnotatedFields(Object bean, AnnotatedBeanProcessor fieldProcessor) {
        ReflectionUtils.doWithFields(bean.getClass(), field -> {
            field.setAccessible(true);

            PropertyId propertyIdAnnotation = field.getAnnotation(PropertyId.class);
            if (Objects.nonNull(propertyIdAnnotation)) {
                try {
                    fieldProcessor.process(bean, field, propertyIdAnnotation);
                } catch (Exception exc) {
                    log.error("Failed to process bean {} field {} with annotation {}",
                            bean,
                            field,
                            propertyIdAnnotation,
                            exc);
                    throw exc;
                }
            }
        });

    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        final long startTime = System.currentTimeMillis();

        doWithAnnotatedFields(bean, (a_bean, field, annotation) -> {
            field.set(bean, processAnnotatedField(a_bean, field, annotation, beanName));
        });

        final long currentProcessingTime = System.currentTimeMillis() - startTime;
        processingTime.add(currentProcessingTime);
        if (log.isDebugEnabled()) {
            log.debug("Resolving @PropertyId annotation for '{}' bean took {} ms. " +
                            "Sum of processing times is equal {} ms now.",
                    beanName, currentProcessingTime, processingTime.sum()
            );
        }

        constructedObjects.put(bean, null);

        return bean;
    }

    private Object processAnnotatedField(Object bean, Field field, PropertyId propertyIdAnnotation, String beanName) {

        Class<?> fieldType = field.getType();
        String propertyId = propertyIdAnnotation.value();
        Class propertyClass = extractPropertyClass(field);
        OptionalDefaultValue<Object> propertyDefaultValue = extractDefaultValue(bean, field);

        if (fieldType.isAssignableFrom(DynamicProperty.class)) {
            //noinspection unchecked
            return new SourcedProperty<>(propertySource, propertyId, propertyClass, propertyDefaultValue);

        } else if (fieldType.isAssignableFrom(PropertySubscription.class)) {
            return new SourcedProperty<>(propertySource, propertyId, propertyClass, propertyDefaultValue)
                    .createSubscription();
        } else {
            log.warn(
                    "@PropertyId annotation is applicable only on fields " +
                            "of DynamicProperty ans PropertySubscription types, not '{}', bean '{}'",
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

    private OptionalDefaultValue<Object> extractDefaultValue(Object bean, Field field) {
        Object propertyValue = null;
        try {
            propertyValue = field.get(bean);
        } catch (IllegalAccessException e) {
            log.error("Error occurred when extracting value from field {}", field.getName());
        }

        if(propertyValue == null)
            return OptionalDefaultValue.none();

        if(propertyValue instanceof DynamicProperty){
            return OptionalDefaultValue.of(((DynamicProperty<?>) propertyValue).get());
        }

        if(propertyValue instanceof PropertySubscription){
            return OptionalDefaultValue.of(((PropertySubscription<?>) propertyValue).get());
        }

        return OptionalDefaultValue.none();
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public boolean requiresDestruction(Object bean) {
        return constructedObjects.containsKey(bean);
    }

    @Override
    public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
        if (!constructedObjects.containsKey(bean)) {
            return;
        }
        doWithAnnotatedFields(bean, (a_bean, field, annotation) -> {
            Object fieldValue = field.get(a_bean);
            if (fieldValue == null) return;

            if (fieldValue instanceof DynamicProperty) {
                ((DynamicProperty<?>) fieldValue).close();
            } else if (fieldValue instanceof PropertySubscription) {
                ((PropertySubscription<?>) fieldValue).close();
            } else {
                log.warn("Invalid field type annotated by @PropertyId." +
                                " Expected DynamicProperty or PropertySubscription." +
                                " Field value will not be closed during bean destruction." +
                                " bean: {}, field {}, PropertyId {}",
                        a_bean.getClass(),
                        field.getName(),
                        annotation.value());
            }

        });
        constructedObjects.remove(bean);
    }
}
