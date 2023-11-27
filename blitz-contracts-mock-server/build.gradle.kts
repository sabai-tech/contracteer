dependencies {
  implementation(project(":blitz-contracts-core"))
  implementation("org.wiremock:wiremock:3.2.0")

  testImplementation(kotlin("test"))
  testImplementation("io.rest-assured:rest-assured:5.3.2")
}

tasks.withType<Test> {
  useJUnitPlatform()
}