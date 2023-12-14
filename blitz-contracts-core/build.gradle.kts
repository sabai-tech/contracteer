dependencies {
    implementation("io.swagger.parser.v3:swagger-parser:2.1.18")

    testImplementation(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

