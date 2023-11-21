plugins {
  kotlin("jvm") version "1.9.0"
  id("java-library")
  id("com.adarshr.test-logger") version "3.2.0"
  id("com.bnorm.power.kotlin-power-assert") version "0.13.0"
}

group = "dev.blitzcraft"
version = System.getenv("RELEASE_VERSION") ?: "LOCAL-SNAPSHOT"

java {
  sourceCompatibility = JavaVersion.VERSION_17
  withSourcesJar()
  withJavadocJar()
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("io.swagger.parser.v3:swagger-parser:2.1.18")
  implementation("net.datafaker:datafaker:2.0.1")
  implementation("com.jayway.jsonpath:json-path:2.8.0")
  implementation("io.rest-assured:rest-assured:5.3.2")
  implementation("org.wiremock:wiremock:3.2.0")
  implementation("org.slf4j:slf4j-simple:2.0.9")


  testImplementation(kotlin("test"))
  testImplementation("io.mockk:mockk:1.13.8")
  testImplementation("org.mock-server:mockserver-netty:5.15.0")
  testImplementation(platform("org.http4k:http4k-bom:5.10.3.0"))
  testImplementation("org.http4k:http4k-core")
  testImplementation("org.http4k:http4k-server-jetty")
}

tasks.withType<Test> {
  useJUnitPlatform()
}


