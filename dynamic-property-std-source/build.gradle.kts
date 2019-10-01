import org.gradle.kotlin.dsl.*

plugins {
    java
    kotlin("jvm")
}

dependencies {

    implementation(Libs.kotlin_stdlib)
    implementation(Libs.kotlin_jdk8)
    implementation(Libs.jfix_stdlib_concurrency){
        exclude("ru.fix", "dynamic-property-api")
    }


    api(project(Projs.dynamic_property_api.dependency))


    testImplementation(Libs.junit_api)
    testRuntimeOnly(Libs.junit_engine)
    testRuntimeOnly(Libs.slf4j_simple)

    testImplementation(Libs.hamkrest)
    testImplementation(Libs.awaitility)

    testImplementation(project(Projs.dynamic_property_jackson.dependency))
}


