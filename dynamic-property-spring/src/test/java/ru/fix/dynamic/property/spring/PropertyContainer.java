package ru.fix.dynamic.property.spring;

import ru.fix.dynamic.property.api.DynamicProperty;

public class PropertyContainer {

    private DynamicProperty<String> defaultCity = DynamicProperty.of("kazan");

    public DynamicProperty<String> getDefaultCity() {
        return defaultCity;
    }
}
