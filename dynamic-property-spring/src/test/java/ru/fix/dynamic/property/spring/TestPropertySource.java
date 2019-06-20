package ru.fix.dynamic.property.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.dynamic.property.api.DynamicPropertyChangeListener;
import ru.fix.dynamic.property.api.DynamicPropertySource;
import ru.fix.dynamic.property.api.converter.DefaultDynamicPropertyMarshaller;
import ru.fix.dynamic.property.api.converter.DynamicPropertyMarshaller;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class TestPropertySource implements DynamicPropertySource {

    private static final Logger logger = LoggerFactory.getLogger(TestPropertySource.class);

    final Properties properties;
    private final DynamicPropertyMarshaller marshaller;

    private Map<String, Collection<DynamicPropertyChangeListener<String>>> listeners = new ConcurrentHashMap<>();

    public TestPropertySource(Properties properties, DynamicPropertyMarshaller marshaller) {
        this.properties = properties;
        this.marshaller = marshaller;
    }

    public String getProperty(String key) {
        return properties.get(key).toString();
    }

    public String getProperty(String key, String defaulValue) {
        return properties.get(key) == null ? defaulValue : properties.get(key).toString();
    }

    @Override
    public <T> T getProperty(String key, Class<T> type) {
        return getProperty(key, type, null);
    }

    @Override
    public <T> T getProperty(String key, Class<T> type, T defaultValue) {
        String value = getProperty(key);
        return marshaller.unmarshall(value, type);
    }

    @Override
    public Properties getAllProperties() {
        return properties;
    }

    @Override
    public Map<String, String> getAllSubtreeProperties(String root) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Properties uploadInitialProperties(String propertiesPath) throws Exception {
        try (InputStream resourceAsStream = getClass().getResourceAsStream(propertiesPath)) {
            properties.load(resourceAsStream);
        }
        return properties;
    }

    @Override
    public void upsertProperty(String key, String propVal) {
        properties.put(key, propVal);
        firePropertyChanged(key, propVal);
    }

    @Override
    public <T> void putIfAbsent(String key, T propVal) throws Exception {
        if (!properties.containsKey(key)) {
            properties.put(key, propVal);
        }
    }

    @Override
    public void updateProperty(String key, String value) {
        properties.put(key, value);
        firePropertyChanged(key, value);
    }

    @Override
    public <T> void addPropertyChangeListener(String propertyName, Class<T> type, DynamicPropertyChangeListener<T>
            typedListener) {
        addPropertyChangeListener(propertyName, value -> {
            T convertedValue = marshaller.unmarshall(value, type);
            typedListener.onPropertyChanged(convertedValue);
        });
    }

    private void addPropertyChangeListener(String propertyName, DynamicPropertyChangeListener<String> listener) {
        listeners.computeIfAbsent(propertyName, key -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @Override
    public void close() {
        // Nothing to do
    }

    private void firePropertyChanged(String propName, String value) {
        Collection<DynamicPropertyChangeListener<String>> zkPropertyChangeListeners = listeners.get(propName);
        if (zkPropertyChangeListeners != null) {
            zkPropertyChangeListeners.forEach(listener -> {
                try {
                    listener.onPropertyChanged(value);
                } catch (Exception e) {
                    logger.error("Failed to update property {}", propName, e);
                }
            });
        }
    }
}
