import java.time.Duration

plugins {
  kotlin("jvm") version "2.1.0"
  kotlin("plugin.power-assert") version "2.0.0"
  id("java-library")
  id("com.adarshr.test-logger") version "4.0.0"
  id("maven-publish")
  id("signing")
  id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

allprojects {
  group = "dev.blitzcraft"
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
          name.set("Blitz-Contracts")
          description.set("Transform your API Spec into Contract Testing")
          url.set("https://blitzcraft.dev")
          licenses {
            license {
              name.set("GNU GENERAL PUBLIC LICENSE, Version 3")
              url.set("https://gnu.org/licenses/gpl-3.0.txt")
            }
          }
          developers {
            developer {
              id.set("blitz-craft")
              name.set("BlitzCraft")
              email.set("contact@blitzcraft.dev")
            }
          }
          scm {
            connection.set("https://github.com/Blitz-Craft/blitz-contracts.git")
            developerConnection.set("git@github.com:Blitz-Craft/blitz-contracts.git")
            url.set("https://github.com/Blitz-Craft/blitz-contracts")
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



