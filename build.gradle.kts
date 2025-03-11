import java.time.Duration

plugins {
  kotlin("jvm") version "2.1.10"
  kotlin("plugin.power-assert") version "2.0.0"
  id("java-library")
  id("com.adarshr.test-logger") version "4.0.0"
  id("maven-publish")
  id("signing")
  id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

allprojects {
  group = "tech.sabai.contracteer"
  version = System.getenv("RELEASE_VERSION") ?: "LOCAL-SNAPSHOT"
  repositories {
    mavenCentral()
  }
}

subprojects {
  apply(plugin = "java-library")
  apply(plugin = "org.jetbrains.kotlin.jvm")
  apply(plugin = "maven-publish")
  apply(plugin = "version-catalog")
  apply(plugin = "signing")
  apply(plugin = "com.adarshr.test-logger")
  apply(plugin = "org.jetbrains.kotlin.plugin.power-assert")
  java {
    toolchain {
      languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
    withJavadocJar()
  }

  publishing {
    publications {
      create<MavenPublication>("jars") {
        from(components["java"])
        pom {
          name.set("Contracteer")
          description.set("Transform your API Spec into Contract Testing")
          url.set("https://sabai.tech")
          licenses {
            license {
              name.set("GNU GENERAL PUBLIC LICENSE, Version 3")
              url.set("https://gnu.org/licenses/gpl-3.0.txt")
            }
          }
          developers {
            developer {
              id.set("sabai-tech")
              name.set("Sabai Tech")
              email.set("contact@bsabai.tech")
            }
          }
          scm {
            connection.set("https://github.com/Sabai-Technologies/contracteer.git")
            developerConnection.set("git@github.com:Sabai-Technologies/contracteer.git")
            url.set("https://github.com/Sabai-Technologies/contracteer")
          }
        }
      }
    }
  }
  signing {
    val signingKey: String? by project
    val signingPassphrase: String? by project
    useInMemoryPgpKeys(signingKey, signingPassphrase)
    sign(publishing.publications["jars"])
  }

}
nexusPublishing {
  this.repositories {
    sonatype {
      nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
      snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
    }
  }
  transitionCheckOptions {
    maxRetries.set(100)
    delayBetween.set(Duration.ofSeconds(5))
  }
}