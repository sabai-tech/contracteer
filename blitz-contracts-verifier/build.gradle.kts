dependencies {
  implementation(project(":blitz-contracts-core"))
  implementation("io.rest-assured:rest-assured:5.3.2")
  implementation("com.jayway.jsonpath:json-path:2.8.0")

  testImplementation(kotlin("test"))
  testImplementation("org.mock-server:mockserver-netty:5.15.0")
  testImplementation("io.mockk:mockk:1.13.8")
  testImplementation(platform("org.http4k:http4k-bom:5.10.3.0"))
  testImplementation("org.http4k:http4k-core")
  testImplementation("org.http4k:http4k-server-jetty")
}

tasks.withType<Test> {
  useJUnitPlatform()
}

