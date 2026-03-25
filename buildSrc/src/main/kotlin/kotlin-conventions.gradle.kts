plugins {
  java
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.power.assert)
  alias(libs.plugins.test.logger)
}

group = "tech.sabai.contracteer"
version = System.getenv("RELEASE_VERSION") ?: "LOCAL-SNAPSHOT"

repositories {
  mavenCentral()
}

java {
  withSourcesJar()
  withJavadocJar()
}

kotlin {
  jvmToolchain(21)
}

dependencies {
  implementation(libs.kotlin.logging)

  testImplementation(kotlin("test"))
  testImplementation(libs.junit.api)
  testImplementation(libs.junit.params)
}

tasks.withType<Test> {
  useJUnitPlatform()
}