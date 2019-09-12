package ru.fix.dynamic.property.api;


import ru.fix.dynamic.property.source.DefaultValue;

public interface DynamicPropertySource extends AutoCloseable {


    //    //TODO: do we need getProperty in interface
//    /**
//     * Returns property value for specified key in required type.
//     * <p>
//     * There are no guarantees of accuracy. This is merely the most recent view
//     * of the data.
//     * </p>
//     * @param key  property name
//     * @param type property type
//     * @return property value, converted to the required type or {@code null} if
//     * there is no such property
//     * @throws IllegalArgumentException if conversation are not supported
//     * @throws NumberFormatException    if property value are not numeric and {@link Integer} or
//     *                                  {@link Long} type are specified
//     */
//    <T> T getProperty(String key, Class<T> type);
//
//    /**
//     * Returns property value for specified key or default.
//     * <p>
//     * There are no guarantees of accuracy. This is merely the most recent view
//     * of the data.
//     * </p>
//     *
//     * @param key          property name
//     * @param type         property type
//     * @param defaultValue default value
//     * @return property value, converted to the required type or
//     * {@code defaultValue} if there is no such property
//     */
//    <T> T getProperty(String key, Class<T> type, T defaultValue);



    interface Subscription extends AutoCloseable {}



//    /**
//     * Registers property change listener. Listener will trigger for
//     * add/update/remove actions on specified property.
//     *
//     * @param propertyName property name to identify property within PropertySource
//     * @param propertyType type of the property
//     * @param defaultValue value that will be used if property does not exist in the store
//     * @param listener     listener
//     */
    <T> Subscription subscribeAndCallListener(
            String propertyName,
            Class<T> propertyType,
            DefaultValue<T> defaultValue,
            DynamicPropertyListener<T> listener);
}
