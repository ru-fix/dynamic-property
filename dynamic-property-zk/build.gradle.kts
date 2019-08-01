import org.gradle.kotlin.dsl.*


plugins {
    java
    kotlin("jvm")
}

dependencies {

    compile(Libs.curator_recipes)
    compile(project(":dynamic-property-api"))
    compile(project(":dynamic-property-jackson"))

    testImplementation(Libs.junit_api)
    testRuntimeOnly(Libs.junit_engine)
    testCompile(Libs.jfix_zookeeper)

    testCompile(Libs.slf4j_simple)
}