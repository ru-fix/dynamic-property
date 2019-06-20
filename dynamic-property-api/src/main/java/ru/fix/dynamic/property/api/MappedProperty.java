package ru.fix.dynamic.property.api;

import java.util.function.Function;

public class MappedProperty<T, R> implements DynamicProperty<R> {

    private final DynamicProperty<T> dynamicProperty;
    private final Function<T, R> map;

    public MappedProperty(DynamicProperty<T> dynamicProperty, Function<T, R> map) {
        this.dynamicProperty = dynamicProperty;
        this.map = map;
    }

    @Override
    public R get() {
        return map.apply(dynamicProperty.get());
    }

    @Override
    public void addListener(DynamicPropertyChangeListener<R> listener) {
        dynamicProperty.addListener(newValue -> listener.onPropertyChanged(map.apply(newValue)));
    }

}
