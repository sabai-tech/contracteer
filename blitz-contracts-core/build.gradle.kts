dependencies {
  implementation("io.swagger.parser.v3:swagger-parser-v3:2.1.24")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")

  testImplementation(kotlin("test"))
}

tasks.withType<Test> {
  useJUnitPlatform()
}

