plugins {
  id ("com.github.johnrengelman.shadow") version "8.1.1"
  kotlin("kapt")
}

dependencies {
  implementation(project(":blitz-contracts-core"))
  implementation(platform("org.http4k:http4k-bom:5.13.2.0"))
  implementation("org.http4k:http4k-core")
  implementation("info.picocli:picocli:4.7.6")

  kapt("info.picocli:picocli-codegen:4.7.6")

  testImplementation(kotlin("test"))
  testImplementation("org.mock-server:mockserver-netty-no-dependencies:5.15.0")
  testImplementation("io.mockk:mockk:1.13.13")
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks.shadowJar {
  archiveClassifier.set("cli")
  manifest.attributes["Main-Class"] = "dev.blitzcraft.contracts.verifier.VerifierCli"
}
