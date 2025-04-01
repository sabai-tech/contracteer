rootProject.name = "contracteer"

include(
  "contracteer-core",
  "contracteer-verifier",
  "contracteer-verifier-junit",
  "contracteer-mockserver",
  "contracteer-mockserver-spring-boot-starter"
)

plugins {
  id("de.fayard.refreshVersions") version "0.60.5"
}