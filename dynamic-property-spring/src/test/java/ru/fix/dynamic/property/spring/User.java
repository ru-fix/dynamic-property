package ru.fix.dynamic.property.spring;

import ru.fix.dynamic.property.api.DynamicProperty;
import ru.fix.dynamic.property.api.annotation.PropertyId;

public class User {
    private String name;

    @PropertyId("property.email")
    private DynamicProperty<String> email;

    public User(String name, DynamicProperty<String> email) {
        this.name = name;
        this.email = email;
    }
}


