plugins {
  id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
  implementation(project(":blitz-contracts-core"))
  implementation("com.github.ajalt.clikt:clikt:4.2.1") //TODO Migrate to picocli:4.7.5"
  implementation(platform("org.http4k:http4k-bom:5.11.1.0"))
  implementation("org.http4k:http4k-core")

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

tasks.register("copyToLib", Copy::class.java) {
  into("$buildDir/libs/lib")
  from(configurations.runtimeClasspath)
}