package ru.fix.dynamic.property.api;

import java.util.Properties;

public interface DynamicPropertySource extends AutoCloseable {

    /**
     * Returns property value for specified key in required type.
     * <p>
     * There are no guarantees of accuracy. This is merely the most recent view
     * of the data.
     * </p>
     * @param key  property name
     * @param type property type
     * @return property value, converted to the required type or {@code null} if
     * there is no such property
     * @throws IllegalArgumentException if conversation are not supported
     * @throws NumberFormatException    if property value are not numeric and {@link Integer} or
     *                                  {@link Long} type are specified
     */
    <T> T getProperty(String key, Class<T> type);

    /**
     * Returns property value for specified key or default.
     * <p>
     * There are no guarantees of accuracy. This is merely the most recent view
     * of the data.
     * </p>
     *
     * @param key          property name
     * @param type         property type
     * @param defaultValue default value
     * @return property value, converted to the required type or
     * {@code defaultValue} if there is no such property
     */
    <T> T getProperty(String key, Class<T> type, T defaultValue);

    /**
     * Reads up all existing properties
     *
     * @return properties with values
     */
    Properties getAllProperties() throws Exception;

    /**
     * Set propertyValue if absent (if node isn't present)
     * <p>
     * If node presents (including empty value), node's value remains unchanged
     *
     * @param key     key
     * @param propVal value
     */
    <T> void putIfAbsent(String key, T propVal) throws Exception;

    /**
     * Registers property change listener. Listener will trigger for
     * add/update/remove actions on specified property.
     *
     * @param propertyName property name
     * @param listener     listener
     */
    <T> void addPropertyChangeListener(String propertyName, Class<T> type, DynamicPropertyListener<T> listener);
}
