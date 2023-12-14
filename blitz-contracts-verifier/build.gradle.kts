plugins {
  id ("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
  implementation(project(":blitz-contracts-core"))
  implementation(platform("org.http4k:http4k-bom:5.10.3.0"))
  implementation("org.http4k:http4k-core")
  implementation("com.jayway.jsonpath:json-path:2.8.0")
  implementation("com.github.ajalt.clikt:clikt:4.2.1")

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

tasks.shadowJar {
  archiveClassifier.set("cli")
  manifest.attributes["Main-Class"] = "dev.blitzcraft.contracts.verifier.VerifierCliKt"
}