import org.gradle.kotlin.dsl.*


plugins {
    java
    kotlin("jvm")
}

dependencies {

    api(project(Projs.dynamic_property_api.dependency))

    implementation(Libs.curator_recipes)
    implementation(Libs.jfix_stdlib_concurrency){
        exclude("ru.fix", "dynamic-property-api")
    }
    implementation(project(Projs.dynamic_property_jackson.dependency))
    implementation(project(Projs.dynamic_property_std_source.dependency))

    testImplementation(Libs.junit_api)
    testRuntimeOnly(Libs.junit_engine)
    testCompile(Libs.jfix_zookeeper)

    testCompile(Libs.slf4j_simple)
}