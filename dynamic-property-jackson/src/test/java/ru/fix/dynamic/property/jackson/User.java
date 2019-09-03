package ru.fix.dynamic.property.jackson;

import java.math.BigInteger;
import java.time.Duration;
import java.util.List;

public class User {
    private String name;
    private Email email;
    private List<BigInteger> phones;
    private Duration sessionDuration;

    public User() {
        // empty for json serialization
    }

    public User(String name, Email email, List<BigInteger> phones, Duration sessionDuration) {
        this.name = name;
        this.email = email;
        this.phones = phones;
        this.sessionDuration = sessionDuration;
    }

    public String getName() {
        return name;
    }

    public Email getEmail() {
        return email;
    }

    public Duration getSessionDuration() {
        return sessionDuration;
    }

}
