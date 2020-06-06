plugins {
    java
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    // Should depend only on slf4j, stdlib-reference and JVM
    api(Libs.slf4j_api)
    api(Libs.jfix_stdlib_reference)
    implementation(Libs.javax_annotation_jsr305)

    testImplementation(Libs.junit_api)
    testImplementation(Libs.junit_engine)
    testImplementation(Libs.kotlin_jdk8)
    testImplementation(Libs.kotlin_stdlib)
    testImplementation(Libs.kotlin_reflect)
    testImplementation(Libs.slf4j_simple)
    testImplementation(Libs.awaitility)
}
