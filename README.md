# dynamic-property
[![Maven Central](https://img.shields.io/maven-central/v/ru.fix/dynamic-property-api.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22ru.fix%22)

Provides easy way to change application configuration at runtime.

```kotlin
class MyService{
    
    @PropertyId("my.service.rate")
    lateinit var rate: DynamicProperty<Integer>
    
    @PropertyId("my.service.setting")
    lateinit var setting: DynamicProperty<Setting> 
    
    init {
        setting.addListener { newSetting ->
            //update service state on settings change  
        }
    }
    
    fun myMethod(){
        val rate = rate.get()
        //use current value of rate from dynamic property
        //next time rate could be changed to another value
        calculate(rate)
    }
}
``` 

## Compose properties
You can build one property based on another:
```kotlin
val stringProperty: DynamicProperty<String>

val intProperty = stringProperty.map { str -> str.toInt() }

val service = ServiceThatRequiresIntProperty(intProperty)
```

## Mock property in tests
```kotlin
//constant property that never changes
val constantProperty = DynamicProperty.of(122)

//property that could change during test
val atomicProperty = AtomicProperty(122)
//... inject properties to service
atomicProperty.set(512)
```