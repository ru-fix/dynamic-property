package ru.fix.dynamic.property.api;

import java.util.Map;
import java.util.Properties;

//TODO: параметры zk
public interface DynamicPropertySource extends AutoCloseable {

    /**
     * Returns property value for specified key.
     * <p>
     * There are no guarantees of accuracy. This is merely the most recent view
     * of the data.
     * </p>
     *
     * @param key property name
     * @return property value or {@code null} if there is no such property
     */
    String getProperty(String key);

    /**
     * Returns property value for specified key.
     * <p>
     * There are no guarantees of accuracy. This is merely the most recent view
     * of the data.
     * </p>
     *
     * @param key         property name
     * @param defaulValue default value for key
     * @return property value or {@code defaultValue} if there is no such
     * property
     */
    String getProperty(String key, String defaulValue);

    /**
     * Returns property value for specified key in required type. Currently
     * supported only {@link String},{@link Integer},{@link Long},
     * {@link Boolean} types.
     * <p>
     * There are no guarantees of accuracy. This is merely the most recent view
     * of the data.
     * </p>
     *
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
     * Works as combination of {@link #getProperty(String, Class)} and
     * {@link #getProperty(String, String)}
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
     * @throws Exception
     */
    Properties getAllProperties() throws Exception;

    /**
     * Read subtree of properties with optional root node.
     *
     * @param root node of subtree, if {@code null} then default config location will be used.
     * @return key-value entries
     * @throws Exception
     */
    Map<String, String> getAllSubtreeProperties(String root) throws Exception;

    /**
     * Uploads to the ZooKeeper cluster initial properties. Wouldn't update node
     * value if it already exists.
     *
     * @param propertiesPath initial properties
     * @return actual set of properties
     * @throws Exception
     */
    Properties uploadInitialProperties(String propertiesPath) throws Exception;

    /**
     * Updates (if already exists) or inserts property.
     *
     * @param key     key
     * @param propVal value
     * @throws Exception
     * @see #updateProperty(String, String)
     */
    void upsertProperty(String key, String propVal) throws Exception;

    /**
     * Set propertyValue if absent (if node isn't present)
     * <p>
     * If node presents (including empty value), node's value remains unchanged
     *
     * @param key     key
     * @param propVal value
     */
    void putIfAbsent(String key, String propVal) throws Exception;

    /**
     * Updates property value
     *
     * @param key   key name
     * @param value new value
     * @throws Exception
     * @see #upsertProperty(String, String)
     */
    void updateProperty(String key, String value) throws Exception;

    /**
     * Registers property change listener. Listener will trigger for
     * add/update/remove actions on specified property.
     *
     * @param propertyName property name
     * @param listener     listener
     */
    void addPropertyChangeListener(String propertyName, DynamicPropertyChangeListener<String> listener);

    /**
     * Registers property change listener. Listener will trigger for
     * add/update/remove actions on specified property.
     *
     * @param propertyName property name
     * @param listener     listener
     */
    <T> void addPropertyChangeListener(String propertyName, Class<T> type, DynamicPropertyChangeListener<T> listener);
}
