package ru.fix.dynamic.property.api.annotation;

import javax.annotation.Nonnull;
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
    @Nonnull
    String value();

}
