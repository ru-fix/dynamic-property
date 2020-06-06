plugins {
    java
    kotlin("jvm")
}

dependencies {

    api(project(Projs.`dynamic-property-api`.dependency))

    implementation(Libs.log4j_kotlin)
    implementation(Libs.curator_recipes)
    implementation(Libs.jfix_stdlib_concurrency) {
        exclude("ru.fix", "dynamic-property-api")
    }
    implementation(project(Projs.`dynamic-property-jackson`.dependency))
    implementation(project(Projs.`dynamic-property-std-source`.dependency))

    testImplementation(Libs.junit_api)
    testRuntimeOnly(Libs.junit_engine)
    testImplementation(Libs.jfix_zookeeper)
    testImplementation(Libs.slf4j_over_log4j)
    testImplementation(Libs.log4j_core)
    testImplementation(Libs.awaitility)
}