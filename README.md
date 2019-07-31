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

## Combine properties
You can build new property based on several others:
```kotlin
val first = AtomicProperty("hello")
val second = AtomicProperty("123")

val combined = CombinedProperty(listOf(first, second)) { first.get() + second.get() }
//combined == "hello123"
        
first.set("hi")
//combined == "hi123"

second.set("42")
//combined == "hi42"
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

## Polled values
DynamicPropertyPoller regularly invoke user defined supplier function that returns current value of DynamicProperty
that backed by custom user defined data source.
```java
DynamicPropertyPoller poller = DynamicPropertyPoller(
        NamedExecutors.newSingleThreadScheduler(
            "polling",
            profiler
        ),
        DynamicProperty.of(Schedule.withRate(1000L)));


class UserClassWithDynamicPropertiesBackedByCustomDataSource {
  DynamicProperty myProperty;

  public UserClass(DynamicPropertyPoller poller){
     myProperty = poller.createProperty(()-> myBattsDatabaseMapper.selectValue(...));
     myProperty.addListener(()->{
         //on property changed
         ...
     });
  }
}
```
