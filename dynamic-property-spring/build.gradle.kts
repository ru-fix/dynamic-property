import org.gradle.kotlin.dsl.*

plugins {
    java
    kotlin("jvm")
}

dependencies {
    api(Libs.spring_beans)
    api(project(Projs.dynamic_property_api.dependency))

    implementation(Libs.slf4j_api)
    implementation(Libs.spring_boot_auto_configure)


    testImplementation(Libs.log4j_kotlin)
    testImplementation(Libs.junit_api)
    testImplementation(Libs.hamkrest)
    testImplementation(Libs.spring_test)
    testRuntimeOnly(Libs.junit_engine)
    testRuntimeOnly(Libs.slf4j_over_log4j)
    testRuntimeOnly(Libs.log4j_core)

    testImplementation(project(Projs.dynamic_property_std_source.dependency))
    testImplementation(project(Projs.dynamic_property_jackson.dependency))
    testImplementation(Libs.jfix_stdlib_concurrency){
        exclude("ru.fix")
    }
    testImplementation(Libs.jfix_stdlib_reference){
        exclude("ru.fix")
    }

}
