dependencies {
  implementation(project(":blitz-contracts-verifier"))
  implementation(project(":blitz-contracts-core"))
  implementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")

  testImplementation(platform("org.http4k:http4k-bom:5.11.1.0"))
  testImplementation("org.http4k:http4k-core")
  testImplementation("org.http4k:http4k-server-jetty")
}

tasks.withType<Test> {
  useJUnitPlatform()
}

