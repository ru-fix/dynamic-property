package ru.fix.dynamic.property.api.annotation;

import javax.annotation.Nonnull;
import java.lang.annotation.*;

/**
 * Unique identifies property within property source
 * @author Kamil Asfandiyarov
 */
@Target(value = {ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PropertyId {

    /**
     * Property identifier
     */
    @Nonnull
    String value();

}
