plugins {
  application
  id("kotlin-conventions")
  alias(libs.plugins.graalvm)
  alias(libs.plugins.shadow)
  kotlin("kapt")
}

dependencies {
  implementation(project(":contracteer-verifier"))
  implementation(project(":contracteer-mockserver"))
  implementation(libs.picocli)
  implementation(libs.logback.classic)

  kapt(libs.picocli.codegen)
}

kapt {
  arguments {
    arg("project", "${project.group}/${project.name}")
  }
}

application {
  mainClass.set("tech.sabai.contracteer.cli.CliKt")
}

graalvmNative {
  binaries {
    named("main") {
      imageName.set("contracteer")
      quickBuild.set(true)
      sharedLibrary.set(false)
      fallback.set(false)
      useFatJar.set(true)
      buildArgs.add("--enable-https")
      buildArgs.add("--enable-http")
    }
  }
}