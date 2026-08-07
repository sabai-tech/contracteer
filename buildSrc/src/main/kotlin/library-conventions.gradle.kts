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
        description.set("The loyal guard of your API contracts. Verify your API and mock your dependencies from your OpenAPI document.")
        url.set("https://contracteer.dev")
        licenses {
          license {
            name.set("Apache License, Version 2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0")
          }
        }
        developers {
          developer {
            id.set("camory")
            name.set("Christophe Amory")
            email.set("christophe@amory.fr")
          }
        }
        scm {
          connection.set("scm:git@github.com:contracteer-dev/contracteer.git")
          developerConnection.set("scm:git@github.com:contracteer-dev/contracteer.git")
          url.set("https://github.com/contracteer-dev/contracteer")
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