plugins {
  `java-library`
  id("kotlin-conventions")
  alias(libs.plugins.shadow)
  kotlin("kapt")
}

dependencies {
  api(project(":contracteer-core"))

  implementation(platform(libs.http4k.bom))
  implementation(libs.http4k.core)
  implementation(libs.picocli)
  implementation(libs.logback.classic)

  kapt(libs.picocli.codegen)

  testImplementation(libs.rest.assured)
}

tasks.shadowJar {
  archiveClassifier.set("cli")
  manifest.attributes["Main-Class"] = "tech.sabai.contracteer.mockserver.MockServerCli"
}