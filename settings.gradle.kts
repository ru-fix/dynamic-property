rootProject.name = "dynamic-property"

for (project in listOf(
        "dynamic-property-jackson",
        "dynamic-property-api",
        "dynamic-property-zk",
        "dynamic-property-spring",
        "dynamic-property-polling",
        "dynamic-property-std-source"
)) {
    include(project)
}
