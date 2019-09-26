package ru.fix.dynamic.property.api.source;

public class DynamicPropertyValueNotFoundException extends RuntimeException {
    public DynamicPropertyValueNotFoundException(String propertyName, Class propertyType) {
        super("" +
                "Property " + propertyName + " of type " + propertyType + " not found in property source." +
                " It does not have default value." +
                " If property was initialized before the old value will stay without any change." +
                " DynamicProperty listeners will not be invoked." +
                " Provide default value for DynamicProperty or configure property value in PropertySource.");
    }
}
