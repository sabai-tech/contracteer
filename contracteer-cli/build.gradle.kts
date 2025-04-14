plugins {
  id("kotlin-conventions")
  alias(libs.plugins.graalvm)
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

graalvmNative {
  binaries {
    named("main") {
      mainClass = "tech.sabai.contracteer.cli.CliKt"
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

tasks.register("generateVersionFile") {
  val outputDir = layout.buildDirectory.dir("generated/resources/version")
  outputs.dir(outputDir)
  doLast {
    val file = outputDir.get().file("version.txt").asFile
    file.parentFile.mkdirs()
    file.writeText(project.version.toString())
  }
}

tasks.named("processResources") {
  dependsOn("generateVersionFile")
}

sourceSets["main"].resources.srcDir(layout.buildDirectory.dir("generated/resources/version"))

