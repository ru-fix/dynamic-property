import org.gradle.kotlin.dsl.*

plugins {
    java
    kotlin("jvm")
}

dependencies {

    implementation(Libs.kotlin_stdlib)
    implementation(Libs.kotlin_jdk8)

    api(project(Projs.dynamic_property_api.dependency))


    testImplementation(Libs.junit_api)
    testRuntimeOnly(Libs.junit_engine)
    testRuntimeOnly(Libs.slf4j_simple)

    testImplementation(Libs.hamkrest)

    testImplementation(project(Projs.dynamic_property_jackson.dependency))
}


