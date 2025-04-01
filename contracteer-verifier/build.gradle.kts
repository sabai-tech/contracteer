plugins {
  `java-library`
  id("kotlin-conventions")
  alias(libs.plugins.shadow)
  alias(libs.plugins.graalvm)
  kotlin("kapt")
}

dependencies {
  api(project(":contracteer-core"))

  implementation(platform(libs.http4k.bom))
  implementation(libs.http4k.core)
  implementation(libs.picocli)
  implementation(libs.logback.classic)

  kapt(libs.picocli.codegen)

  testImplementation(libs.mockserver.netty)
  testImplementation(libs.mockk)
}


tasks.shadowJar {
  archiveClassifier.set("cli")
  manifest.attributes["Main-Class"] = "tech.sabai.contracteer.verifier.VerifierCli"
}

graalvmNative {
  toolchainDetection.set(true)
  binaries {
    named("main") {
      mainClass.set("tech.sabai.contracteer.verifier.VerifierCli")
      sharedLibrary.set(false)
      fallback.set(false)
      useFatJar.set(true)
    }
  }
}