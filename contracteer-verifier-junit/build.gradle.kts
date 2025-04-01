plugins {
  `java-library`
  id("kotlin-conventions")
}

dependencies {
  implementation(project(":contracteer-verifier"))
  implementation(libs.junit.engine)

  testImplementation(platform(libs.http4k.bom))
  testImplementation(libs.http4k.core)
  testImplementation(libs.http4k.jetty)
  testImplementation(libs.logback.classic)
}

