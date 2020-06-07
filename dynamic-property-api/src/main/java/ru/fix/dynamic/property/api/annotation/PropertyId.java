package ru.fix.dynamic.property.api.annotation;

import javax.annotation.Nonnull;
import java.lang.annotation.*;

/**
 * Unique identifies property within property source
 * Used in automatic injection scenarios
 * <pre>{@code
 * class MyService{
 *   @PropertyId("my.service.timeout)
 *   DynamicProperty<Integer> timeout;
 *
 *   @PropertyId("my.service.settings)
 *   PropertySubscription<Settings> settings;
 * }
 * }</pre>
 *
 * You can provide default values by explicit field initialization:
 * <pre>{@code
 * class MyService{
 *   @PropertyId("my.service.timeout)
 *   DynamicProperty<Integer> timeout = DynamicProperty.of(120);
 * }
 * }</pre>
 *
 * After dependency injection phase completion you can subscribe to dynamic property change by listeners:
 * <pre>{@code
 * class MyService{
 *   @PropertyId("my.service.timeout)
 *   DynamicProperty<Integer> timeout = DynamicProperty.of(120);
 *   PropertySubscription<Integer> timeoutSubscription;
 *
 *   @PostConstruct
 *   void initialize(){
 *     timeoutSubscription = timeout.createSubscription().setAndCallListener((oldValue, newValue)->{...})
 *   }
 * }
 * }</pre>
 *
 * Or use simplified version with automatic subscription:
 * <pre>{@code
 * class MyService{
 *   @PropertyId("my.service.timeout)
 *   PropertySubscription<Integer> timeout = PropertySubscription.of(120);
 * }
 * }</pre>
 *
 * Be aware that dynamic property field will be replaced with new one during dependency injection process.
 * Usually dependency injection framework inject dependencies after object construction.
 * Do not use or subscribe to DynamicProperty instances that are temporal and works only as a default value provider
 * for a dependency injection frameworks.
 * <pre>{@code
 * // DO NOT DO THAT!
 * class MyService{
 *   @PropertyId("my.service.timeout)
 *   DynamicProperty<Integer> timeout = DynamicProperty.of(120);
 *   PropertySubscription<Integer> timeoutSubscription;
 *   MyService(){
 *     //Constructor sees 'default' instance of DynamicProperty with value 120
 *     //But this instance will be replaced with another one during dependency injection phase
 *     //All subscription will be lost and listeners will no longer be invoked
 *     timeoutSubscription = timeout.createSubscription()
 *                      .setAndCallListener((oldValue, newValue)->{...})
 *   }
 * }
 * }</pre>
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
