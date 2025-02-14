dependencies {
  implementation(project(":contracteer-verifier"))
  implementation("org.junit.jupiter:junit-jupiter-engine:5.11.3")
  implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

  testImplementation(kotlin("test"))
  testImplementation(platform("org.http4k:http4k-bom:5.38.0.0"))
  testImplementation("org.http4k:http4k-core")
  testImplementation("org.http4k:http4k-server-jetty")
  testImplementation ("ch.qos.logback:logback-classic:1.5.16")
}

tasks.withType<Test> {
  useJUnitPlatform()
}

