dependencies {
    implementation("io.swagger.parser.v3:swagger-parser:2.1.18")
    implementation("net.datafaker:datafaker:2.0.1")

    testImplementation(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

