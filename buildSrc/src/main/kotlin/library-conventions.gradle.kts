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
        description.set("The loyal guard of your API contracts. Verify your API and mock your dependencies from your OpenAPI specification.")
        url.set("https://github.com/sabai-tech/contracteer")
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
          connection.set("scm:git@github.com:sabai-tech/contracteer.git")
          developerConnection.set("scm:git@github.com:sabai-tech/contracteer.git")
          url.set("https://github.com/sabai-tech/contracteer")
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