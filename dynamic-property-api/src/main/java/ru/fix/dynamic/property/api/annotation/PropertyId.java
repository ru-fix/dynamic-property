package ru.fix.dynamic.property.api.annotation;

import java.lang.annotation.*;

/**
 * @author Kamil Asfandiyarov
 */
@Target(value = {ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PropertyId {

    /**
     * Property id
     */
    String value();

}
