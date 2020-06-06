plugins {
    java
    kotlin("jvm")
}

dependencies {

    implementation(Libs.kotlin_stdlib)
    implementation(Libs.kotlin_jdk8)
    implementation(Libs.jfix_stdlib_concurrency) {
        exclude("ru.fix", "dynamic-property-api")
    }
    implementation(Libs.jfix_stdlib_files) {
        exclude("ru.fix", "dynamic-property-api")
    }

    implementation(Libs.log4j_kotlin)

    api(project(Projs.`dynamic-property-api`.dependency))


    testImplementation(Libs.junit_api)
    testRuntimeOnly(Libs.junit_engine)
    testRuntimeOnly(Libs.slf4j_simple)

    testImplementation(Libs.hamkrest)
    testImplementation(Libs.awaitility)

    testImplementation(project(Projs.`dynamic-property-jackson`.dependency))
}
