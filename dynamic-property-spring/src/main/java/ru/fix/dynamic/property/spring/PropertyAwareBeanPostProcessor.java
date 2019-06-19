package ru.fix.dynamic.property.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;
//import ru.fix.commons.zkconfig.api.FinalZkConfig;
//import ru.fix.commons.zkconfig.api.ZkConfig;
import ru.fix.dynamic.property.api.DynamicPropertySource;
import ru.fix.dynamic.property.api.annotation.DynamicPropertyDescription;
import ru.fix.dynamic.property.zk.ValueConverter;
//import ru.fix.dynamic.property.zk.ZKConfig;
import ru.fix.dynamic.property.zk.ZKConfigPropertyHolder;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author Kamil Asfandiyarov
 */
public class PropertyAwareBeanPostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(PropertyAwareBeanPostProcessor.class);

    private DynamicPropertySource propertySource;
    private LongAdder processingTime = new LongAdder();

    public PropertyAwareBeanPostProcessor(DynamicPropertySource propertySource) {
        this.propertySource = propertySource;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        final long startTime = System.currentTimeMillis();

        ReflectionUtils.doWithFields(bean.getClass(), field -> {

            field.setAccessible(true);

            DynamicPropertyDescription zkConfigAnnotation = field.getAnnotation(DynamicPropertyDescription.class);
            if (Objects.nonNull(zkConfigAnnotation)) {
                field.set(bean, processZKConfigAnnotation(field, zkConfigAnnotation, beanName));
            }

//          TODO: final пока отсуствует
//            FinalZkConfig finalZkConfigAnnotation = field.getAnnotation(FinalZkConfig.class);
//            if (Objects.nonNull(finalZkConfigAnnotation)) {
//                field.set(bean, processFinalZKConfigAnnotation(field.getType(), finalZkConfigAnnotation));
//            }

        });

        final long currentProcessingTime = System.currentTimeMillis() - startTime;
        processingTime.add(currentProcessingTime);
        log.debug("Resolving zk annotation for \"{}\" bean took {} ms. Sum of processing times is equal {} ms now.",
                beanName, currentProcessingTime, processingTime.sum());

        return bean;
    }

//    private Object processFinalZKConfigAnnotation(Class<?> fieldType, FinalZkConfig finalZkConfigAnnotation) {
//
//        String zkConfigName = finalZkConfigAnnotation.name();
//        String zkConfigDefaultValue = finalZkConfigAnnotation.defaultValue();
//        String zkConfigDescription = finalZkConfigAnnotation.description();
//
//        upsertZkConfig(zkConfigDefaultValue, zkConfigName, zkConfigDescription, true);
//
//        Object object = propertySource.getProperty(zkConfigName, fieldType);
//        return object == null ? ValueConverter.convert(fieldType, zkConfigDefaultValue, null) : object;
//    }

    private Object processZKConfigAnnotation(Field field, DynamicPropertyDescription zkConfigAnnotation, String beanName) {

        Class<?> fieldType = field.getType();

        if (fieldType.isAssignableFrom(ZKConfigPropertyHolder.class)) {

            String zkConfigId = zkConfigAnnotation.id();
            String zkConfigDefaultValue = zkConfigAnnotation.defaultValue();
            String zkConfigDescription = zkConfigAnnotation.description();

            ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
            Type type = parameterizedType.getActualTypeArguments()[0];
            Class propertyClass;
            if (type instanceof ParameterizedType) {
                propertyClass = (Class) ((ParameterizedType) type).getRawType();
            } else {
                propertyClass = (Class) type;
            }

            upsertZkConfig(zkConfigDefaultValue, zkConfigId, zkConfigDescription, false);

            Object defaultValue = ValueConverter.convert(propertyClass, zkConfigDefaultValue, null);

            return new ZKConfigPropertyHolder<>(propertySource, zkConfigId, propertyClass, defaultValue);

        } else {
            //TODO: надо пересмотреть лог, что будет писать?
            log.warn("ZkConfig annotation is applicable only on fields of ZKConfigPropertyHolder type, not '{}'," +
                    " bean name - '{}'.", fieldType, beanName);
            return null;
        }
    }

    //TODO:  по поводу обновления, либо работать с имеющимися
    private void upsertZkConfig(String zkConfigDefaultValue, String zkConfigName, String zkConfigDescription, boolean rebootRequired) {

        if (zkConfigDefaultValue != null) {
            try {
                propertySource.putIfAbsent(zkConfigName, zkConfigDefaultValue);
            } catch (Exception e) {
                log.error("Error creating zkPropertry '{}' into property '{}'", zkConfigName, zkConfigDefaultValue, e);
            }
        }

        try {
            propertySource.upsertProperty(zkConfigName + "/_INFO", zkConfigDescription);
            propertySource.upsertProperty(zkConfigName + "/_REBOOT_REQUIRED", String.valueOf(rebootRequired));
        } catch (Exception e) {
            log.error("error upserting zkPropertry '{}'", zkConfigName, e);
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
