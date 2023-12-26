dependencies {
  implementation("io.swagger.parser.v3:swagger-parser-v3:2.1.19")

  testImplementation(kotlin("test"))
}

tasks.withType<Test> {
  useJUnitPlatform()
}

