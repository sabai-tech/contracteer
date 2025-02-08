plugins {
  id("com.github.johnrengelman.shadow") version "8.1.1"
  kotlin("kapt")
}

dependencies {
  api(project(":contracteer-core"))

  implementation(platform("org.http4k:http4k-bom:5.38.0.0"))
  implementation("org.http4k:http4k-core")
  implementation("info.picocli:picocli:4.7.6")
  implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

  kapt("info.picocli:picocli-codegen:4.7.6")

  testImplementation ("ch.qos.logback:logback-classic:1.5.16")
  testImplementation(kotlin("test"))
  testImplementation("io.rest-assured:rest-assured:5.5.0")
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks.shadowJar {
  archiveClassifier.set("cli")
  manifest.attributes["Main-Class"] = "tech.sabai.contracteer.mockserver.MockServerCli"
}