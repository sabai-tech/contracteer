dependencies {
  implementation("io.swagger.parser.v3:swagger-parser-v3:2.1.24")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
  implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
  implementation ("ch.qos.logback:logback-classic:1.5.16")

  testImplementation(kotlin("test"))
}

tasks.withType<Test> {
  useJUnitPlatform()
}

