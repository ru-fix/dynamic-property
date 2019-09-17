package ru.fix.dynamic.property.source;

public class DefaultValue<T>{
    final Boolean isPresent;
    final T value;

    private DefaultValue(Boolean isPresent, T value) {
        this.isPresent = isPresent;
        this.value = value;
    }

    public static <T> DefaultValue<T> of(T value) {
        return new DefaultValue<>(true, value);
    }
    public static <T> DefaultValue<T> none(){
        return new DefaultValue<>(false, null);
    }

    public Boolean isPresent(){
        return isPresent;
    }

    public T get() {
        if(!isPresent) throw new IllegalStateException("Default value is not present");
        return value;
    }
}
