plugins {
  id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
  implementation(project(":blitz-contracts-core"))
  implementation("org.wiremock:wiremock:3.2.0")
  implementation("com.github.ajalt.clikt:clikt:4.2.1")

  testImplementation(kotlin("test"))
  testImplementation("io.rest-assured:rest-assured:5.3.2")
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks.shadowJar {
  archiveClassifier.set("cli")
  manifest.attributes["Main-Class"] = "dev.blitzcraft.contracts.mockserver.MockServerCliKt"
}