package ru.fix.dynamic.property.spring;

import ru.fix.dynamic.property.api.DynamicPropertyListener;
import ru.fix.dynamic.property.api.DynamicPropertySource;
import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class TestPropertySource implements DynamicPropertySource {

    final Properties properties;
    private final DynamicPropertyMarshaller marshaller;

    private Map<String, Collection<DynamicPropertyListener<String>>> listeners = new ConcurrentHashMap<>();

    public TestPropertySource(Properties properties, DynamicPropertyMarshaller marshaller) {
        this.properties = properties;
        this.marshaller = marshaller;
    }

    public String getProperty(String key) {
        return Optional.ofNullable(properties.get(key))
                .map(Object::toString)
                .orElse(null);
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
    public <T> void addPropertyChangeListener(String propertyName, Class<T> type, DynamicPropertyListener<T>
            typedListener) {
        addPropertyChangeListener(propertyName, value -> {
            T convertedValue = marshaller.unmarshall(value, type);
            typedListener.onPropertyChanged(convertedValue);
        });
    }

    private void addPropertyChangeListener(String propertyName, DynamicPropertyListener<String> listener) {
        listeners.computeIfAbsent(propertyName, key -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @Override
    public void close() {
        // Nothing to do
    }
}
