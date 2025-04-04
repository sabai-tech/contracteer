plugins {
  id("kotlin-conventions")
  `java-library`
  `maven-publish`
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])
      pom {
        name.set("contracteer")
        description.set("A tool for validating API contracts and running mock servers based on OpenAPI 3 Specifications")
        url.set("https://github.com/Sabai-Technologies/contracteer")
        licenses {
          license {
            name.set("GNU General Public License, Version 3")
            url.set("https://www.gnu.org/licenses/gpl-3.0.html")
          }
        }
        developers {
          developer {
            id.set("camory")
            name.set("Christophe Amory")
            email.set("christophe@sabai.tech")
          }
        }
        scm {
          connection.set("scm:git@github.com:Sabai-Technologies/contracteer.git")
          developerConnection.set("scm:git@github.com:Sabai-Technologies/contracteer.git")
          url.set("https://github.com/Sabai-Technologies/contracteer")
        }
      }
    }
  }

  repositories {
    maven {
      name = "staging"
      url = rootProject.layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
    }
  }
}