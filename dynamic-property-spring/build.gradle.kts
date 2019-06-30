import org.gradle.kotlin.dsl.*

plugins {
    java
    kotlin("jvm")
}

dependencies {

    compile(Libs.spring_beans)
    compile(Libs.spring_boot_auto_configure)

    compile(project(":dynamic-property-zk"))
    compile(project(":dynamic-property-api"))
    compile(project(":dynamic-property-jackson"))

    testImplementation(Libs.junit_api)
    testRuntimeOnly(Libs.junit_engine)
    testCompile(Libs.spring_test)
}


