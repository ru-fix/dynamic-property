package ru.fix.dynamic.property.source;

public class DynamicPropertyNotFoundException extends RuntimeException {
    public DynamicPropertyNotFoundException(String propertyName, Class propertyType) {
        super("" +
                "Property " + propertyName + " of type " + propertyType + " not found in property source." +
                " It does not have default value." +
                " Provide default value or configure property in PropertySource.");
    }
}
