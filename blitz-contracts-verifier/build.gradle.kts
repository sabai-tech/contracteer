plugins {
  id ("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
  implementation(project(":blitz-contracts-core"))
  implementation(platform("org.http4k:http4k-bom:5.11.1.0"))
  implementation("org.http4k:http4k-core")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")
  implementation("com.github.ajalt.clikt:clikt:4.2.1") //TODO Migrate to picocli:4.7.5"

  testImplementation(kotlin("test"))
  testImplementation("org.mock-server:mockserver-netty:5.15.0")
  testImplementation("io.mockk:mockk:1.13.8")
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks.shadowJar {
  archiveClassifier.set("cli")
  manifest.attributes["Main-Class"] = "dev.blitzcraft.contracts.verifier.VerifierCliKt"
}