plugins {
  id("com.github.johnrengelman.shadow") version "8.1.1"
  kotlin("kapt")
}

dependencies {
  implementation(project(":blitz-contracts-core"))
  implementation(platform("org.http4k:http4k-bom:5.13.2.0"))
  implementation("org.http4k:http4k-core")
  implementation("info.picocli:picocli:4.7.5")

  kapt("info.picocli:picocli-codegen:4.7.5")

  testImplementation(kotlin("test"))
  testImplementation("io.rest-assured:rest-assured:5.3.2")
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks.shadowJar {
  archiveClassifier.set("cli")
  manifest.attributes["Main-Class"] = "dev.blitzcraft.contracts.mockserver.MockServerCli"
}

kapt {
  arguments {
    arg("project", "${project.group}/${project.name}")
  }
}