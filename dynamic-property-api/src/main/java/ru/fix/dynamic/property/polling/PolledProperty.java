package ru.fix.cpapsm.commons.poll;

/**
 *
 * @author Andrey Kiselev
 */

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

 import ru.fix.dynamic.property.api.DynamicProperty;
import ru.fix.dynamic.property.api.AtomicProperty;
import ru.fix.dynamic.property.api.DynamicPropertyListener;

public class PolledProperty<PolledType> extends AtomicProperty<PolledType>
    implements DynamicProperty<PolledType>  {
    private final Supplier<PolledType> retriever;

    public PolledProperty(Supplier<PolledType> retriever) {
        super(retriever.get());
        this.retriever = retriever;
    }

    public void poll() {
        PolledType pValue = retriever.get();
        if( ! Objects.equals(pValue, get())) {
            set(pValue);
        }
    }
}
