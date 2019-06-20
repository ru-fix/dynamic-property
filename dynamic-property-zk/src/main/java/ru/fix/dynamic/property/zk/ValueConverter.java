package ru.fix.dynamic.property.zk;

//import ru.fix.cpapsm.commons.lang.marshall.Marshaller;

import ru.fix.dynamic.property.api.converter.JSonPropertyMarshaller;

import java.math.BigDecimal;

public class ValueConverter {

    private ValueConverter() {
        throw new IllegalStateException("Utility class");
    }

    @SuppressWarnings("unchecked")
    public static <T> T convert(Class<T> type, String value, T defaultValue) {
        if (value == null) {
            return defaultValue;
        } else if (Byte.class.equals(type) || byte.class.equals(type)) {
            return (T) Byte.valueOf(value);
        } else if (Integer.class.equals(type) || int.class.equals(type)) {
            return (T) Integer.valueOf(value);
        } else if (Boolean.class.equals(type) || boolean.class.equals(type)) {
            return (T) Boolean.valueOf(value);
        } else if (Long.class.equals(type) || long.class.equals(type)) {
            return (T) Long.valueOf(value);
        } else if (Double.class.equals(type) || double.class.equals(type)) {
            return (T) Double.valueOf(value);
        } else if (BigDecimal.class.equals(type)) {
            return (T) new BigDecimal(value);
        } else if (String.class.equals(type)) {
            return (T) value;
        } else {
            return new JSonPropertyMarshaller().unmarshall(value, type);
        }
   }
}
