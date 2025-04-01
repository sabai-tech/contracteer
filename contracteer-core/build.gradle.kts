plugins {
  `java-library`
  id("kotlin-conventions")
}

dependencies {
  implementation(libs.swagger.parser)
  implementation(libs.jackson.databind)

  testImplementation(libs.logback.classic)
  testImplementation(libs.mockserver.netty)
}

