package ru.fix.dynamic.property.spring.exception;

public class DynamicPropertyDefaultValueNotDefinedException extends RuntimeException {
    public DynamicPropertyDefaultValueNotDefinedException(String message) {
        super(message);
    }
}
