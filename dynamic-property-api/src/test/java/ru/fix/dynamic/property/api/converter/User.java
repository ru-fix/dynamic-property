package ru.fix.dynamic.property.api.converter;

import java.math.BigInteger;
import java.util.List;

public class User {
    private String name;
    private Email email;
    private List<BigInteger> phones;

    public User() {
        // empty for json serialization
    }

    public User(String name, Email email, List<BigInteger> phones) {
        this.name = name;
        this.email = email;
        this.phones = phones;
    }

    public String getName() {
        return name;
    }

    public Email getEmail() {
        return email;
    }


}
