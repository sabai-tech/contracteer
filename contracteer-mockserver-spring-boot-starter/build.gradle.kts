plugins {
  id("org.springframework.boot") version "3.4.2"
  id("org.jetbrains.kotlin.plugin.spring") version "2.1.0"
}

apply(plugin = "io.spring.dependency-management")

dependencies {
  implementation(project(":contracteer-mockserver"))
  implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

  implementation("org.springframework.boot:spring-boot-test")
  implementation("org.springframework:spring-test")

  testImplementation(kotlin("test"))
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("io.rest-assured:rest-assured:5.5.0")
}

tasks.bootJar {
  enabled = false
}

tasks.jar {
  archiveClassifier.set("")
}

tasks.withType<Test> {
  useJUnitPlatform()
}
