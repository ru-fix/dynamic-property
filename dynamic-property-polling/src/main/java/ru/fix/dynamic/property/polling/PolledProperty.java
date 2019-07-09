package ru.fix.dynamic.property.polling;

/**
 *
 * @author Andrey Kiselev
 */

import java.util.Objects;
import java.util.function.Supplier;

import ru.fix.dynamic.property.api.DynamicProperty;
import ru.fix.dynamic.property.api.AtomicProperty;

public class PolledProperty<T> extends AtomicProperty<T>
    implements DynamicProperty<T>  {
    private final Supplier<T> retriever;

    public PolledProperty(Supplier<T> retriever) {
        super(retriever.get());
        this.retriever = retriever;
    }

    public void poll() {
        T pValue = retriever.get();
        if( ! Objects.equals(pValue, get())) {
            set(pValue);
        }
    }
}
