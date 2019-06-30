package ru.fix.dynamic.property.jackson;

public class Email {
    private String value;

    public Email() {
        // empty for json serialization
    }


    public Email(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
