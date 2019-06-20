package ru.fix.dynamic.property.spring;

import org.junit.jupiter.api.Test;
import ru.fix.dynamic.property.api.DynamicProperty;
import ru.fix.dynamic.property.api.annotation.PropertyId;
import ru.fix.dynamic.property.api.converter.JSonPropertyMarshaller;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class TestReflection {



    @Test
    public void str() {

        String str = "1";

        Long str1 = new JSonPropertyMarshaller().unmarshall(str, Long.class);
    }



    @Test
    public void test() throws NoSuchFieldException, IllegalAccessException {

        User user = new User("Vladislav", DynamicProperty.of("Email"));

        Class<?> clazz = user.getClass();

        for (Field declaredField : clazz.getDeclaredFields()) {

            declaredField.setAccessible(true);

            if (declaredField.isAnnotationPresent(PropertyId.class)) {

                ParameterizedType parameterizedType = (ParameterizedType) declaredField.getGenericType();
                Type type = parameterizedType.getActualTypeArguments()[0];
                Class propertyClass;
                if (type instanceof ParameterizedType) {
                    propertyClass = (Class) ((ParameterizedType) type).getRawType();
                } else {
                    propertyClass = (Class) type;
                }

                DynamicProperty<?> object = (DynamicProperty<?>) declaredField.get(user);
                System.out.println(propertyClass);
                System.out.println(new JSonPropertyMarshaller().marshall(object.get()));
            }
        }

    }
}
