plugins {
  id("library-conventions")
  `java-test-fixtures`
}

dependencies {
  implementation(libs.swagger.parser)
  implementation(libs.jackson.databind)
  implementation(libs.rgxgen)

  testImplementation(libs.logback.classic)
  testImplementation(libs.mockserver.netty)
}

