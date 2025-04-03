rootProject.name = "contracteer"

include(
  "contracteer-cli",
  "contracteer-core",
  "contracteer-mockserver",
  "contracteer-mockserver-spring-boot-starter",
  "contracteer-verifier",
  "contracteer-verifier-junit",
)

plugins {
  id("de.fayard.refreshVersions") version "0.60.5"
}