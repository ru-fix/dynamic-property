package ru.fix.dynamic.property.spring;

import ru.fix.dynamic.property.api.DynamicProperty;
import ru.fix.dynamic.property.api.annotation.PropertyId;

public class PropertyContainer {

    private DynamicProperty<String> status = DynamicProperty.of("NEW");

    @PropertyId("property.city")
    private DynamicProperty<String> city = DynamicProperty.of("kazan");

    public DynamicProperty<String> getCity() {
        return city;
    }

    public DynamicProperty<String> getStatus() {
        return status;
    }
}
