package ru.fix.dynamic.property.api.source;

public class OptionalDefaultValue<T> {
    final Boolean isPresent;
    final T value;

    private OptionalDefaultValue(Boolean isPresent, T value) {
        this.isPresent = isPresent;
        this.value = value;
    }

    public static <T> OptionalDefaultValue<T> of(T value) {
        return new OptionalDefaultValue<>(true, value);
    }

    public static <T> OptionalDefaultValue<T> none() {
        return new OptionalDefaultValue<>(false, null);
    }

    public Boolean isPresent() {
        return isPresent;
    }

    public T get() {
        if (!isPresent) throw new IllegalStateException("Default value is not present");
        return value;
    }
}
