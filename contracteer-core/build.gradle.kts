dependencies {
  implementation("io.swagger.parser.v3:swagger-parser-v3:2.1.24")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
  implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

  testImplementation ("ch.qos.logback:logback-classic:1.5.16")
  testImplementation(kotlin("test"))
  testImplementation("org.mock-server:mockserver-netty-no-dependencies:5.15.0")
}

kotlin {
  compilerOptions {
    freeCompilerArgs.add("-Xwhen-guards")
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
}

