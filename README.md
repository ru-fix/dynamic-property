# dynamic-property
[![Maven Central](https://img.shields.io/maven-central/v/ru.fix/dynamic-property-api.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22ru.fix%22)

Provides easy way to change application configuration at runtime.

```kotlin
class MyService{
    
    @PropertyId("my.service.rate")
    lateinit var rate: DynamicProperty<Integer>
    
    fun myMethod(){
        val rate = rate.get()
        //use current value of rate from dynamic property
        calculate(rate)
    }
}
```

```kotlin
class MyService {
    
    @PropertyId("my.service.setting")
    lateinit var setting: PropertySubscription<Setting>

    @PostConstruct
    fun postConstructInitialization() {
        setting.setAndCallListener{oldValue, newValue ->
            updateState(newValue)
        }       
    }
    fun updateState(setting: Setting) {}
    fun doWork() {}
}
```


```kotlin
class MyService(setting: DynamicProperty<Setting>) {
    val subscription = setting.createSubscription().setAndCallListener { oldSetting, newSetting ->
        // initialize service
        // or update state on settings change
        updateState(newSetting)
    }

    fun updateState(setting: Setting) {}
    fun doWork() {}
}
```

```kotlin
class MyService(val setting: DynamicProperty<Setting>) {
    fun doWork() {
        val currentSetting = setting.get()
    }
}
```

Support various property sources.
 * properties files
 * ZooKeeper

![](docs/dynamic-properties.png?raw=true)

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
val myService = MyService(constantProperty)
myService.doWork()

//property that could change during test
val atomicProperty = AtomicProperty(122)
val myService = MyService(atomicProperty)
atomicProperty.set(512)
myService.doWork()
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
  DynamicProperty<String> myProperty;

  public UserClass(DynamicPropertyPoller poller){
     myProperty = poller.createProperty(()-> myDaoService.loadValueFromDatabase(...));
     myProperty.addAndCallListener( value -> {
         //init or update state based on property value
         ...
     });
  }
}
```
