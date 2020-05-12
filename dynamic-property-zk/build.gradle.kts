import org.gradle.kotlin.dsl.*


plugins {
    java
    kotlin("jvm")
}

dependencies {

    api(project(Projs.dynamic_property_api.dependency))

    implementation(Libs.log4j_kotlin)
    implementation(Libs.curator_recipes)
    implementation(Libs.jfix_stdlib_concurrency){
        exclude("ru.fix", "dynamic-property-api")
    }
    implementation(Libs.jfix_stdlib_reference){
        exclude("ru.fix", "dynamic-property-api")
    }
    implementation(project(Projs.dynamic_property_jackson.dependency))
    implementation(project(Projs.dynamic_property_std_source.dependency))

    testImplementation(Libs.junit_api)
    testRuntimeOnly(Libs.junit_engine)
    testImplementation(Libs.jfix_zookeeper)
    testImplementation(Libs.slf4j_over_log4j)
    testImplementation(Libs.log4j_core)
    testImplementation(Libs.awaitility)
}