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
  gradlePluginPortal()
}

java {
  withSourcesJar()
  withJavadocJar()
}

kotlin {
  jvmToolchain(21)
  compilerOptions {
    freeCompilerArgs.add("-Xwhen-guards")
  }
}

dependencies {
  implementation(libs.kotlin.logging)

  testImplementation(kotlin("test"))
  testImplementation(libs.junit.api)
}

tasks.withType<Test> {
  useJUnitPlatform()
}