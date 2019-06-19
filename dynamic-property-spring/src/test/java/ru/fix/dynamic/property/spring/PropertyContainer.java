package ru.fix.dynamic.property.spring;

import ru.fix.dynamic.property.api.DynamicProperty;
import ru.fix.dynamic.property.api.annotation.DynamicPropertyDescription;

public class PropertyContainer {

    @DynamicPropertyDescription(
            id = "property.id",
            description = "opisanie",
            defaultValue = "Kazan"
    )
    private DynamicProperty<String> defaultCity;

    public DynamicProperty<String> getDefaultCity() {
        return defaultCity;
    }
}
