plugins {
  id("kotlin-conventions")
  alias(libs.plugins.graalvm)
  kotlin("kapt")
  distribution
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

graalvmNative {
  binaries {
    named("main") {
      mainClass = "tech.sabai.contracteer.cli.CliKt"
      imageName.set("contracteer-${project.version}")
      quickBuild.set(true)
      sharedLibrary.set(false)
      fallback.set(false)
      useFatJar.set(true)
      buildArgs.add("--enable-https")
      buildArgs.add("--enable-http")
    }
  }
}