plugins {
  id("kotlin-conventions")
  `java-library`
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.kotlin.spring)
}

apply(plugin = "io.spring.dependency-management")

dependencies {
  implementation(project(":contracteer-mockserver"))
  implementation(libs.spring.boot.test)
  implementation(libs.spring.test)

  testImplementation(libs.spring.boot.starter.test)
  testImplementation(libs.rest.assured)
}

tasks.bootJar {
  enabled = false
}

tasks.jar {
  archiveClassifier.set("")
}
