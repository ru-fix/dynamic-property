package ru.fix.dynamic.property.spring.exception;

public class DynamicPropertyDefaultValueNotFoundException extends RuntimeException {
    public DynamicPropertyDefaultValueNotFoundException(String message) {
        super(message);
    }
}
