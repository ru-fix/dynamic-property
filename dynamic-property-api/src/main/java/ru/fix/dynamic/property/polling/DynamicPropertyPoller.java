package ru.fix.dynamic.property.polling;

/**
 *
 * @author Andrey Kiselev
 */

import java.util.function.Supplier;
import java.util.WeakHashMap;

public class DynamicPropertyPoller implements Runnable {
    private WeakHashMap<PolledProperty, Supplier> properties = new WeakHashMap<>();
        
    @Override
    public void run() {
        properties.forEach((k, v) ->  k.poll());
    }
    
    public <DType> PolledProperty<DType> createProperty(Supplier<DType> retriever) {
        PolledProperty<DType> property = new PolledProperty<>(retriever);
        property.poll();
        properties.put(property, retriever);
        
        return property;
    }

    public void deleteProperty(PolledProperty pp) {
        properties.remove(pp);
    }
}
