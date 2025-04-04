plugins {
  id("library-conventions")
}

dependencies {
  api(project(":contracteer-core"))

  implementation(platform(libs.http4k.bom))
  implementation(libs.http4k.core)

  testImplementation(libs.mockserver.netty)
  testImplementation(libs.mockk)
}