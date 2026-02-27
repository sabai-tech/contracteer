rootProject.name = "contracteer"

include(
  "contracteer-cli",
  "contracteer-core",
  "contracteer-mockserver",
  "contracteer-mockserver-spring",
  "contracteer-verifier",
  "contracteer-verifier-junit",
)

plugins {
  id("de.fayard.refreshVersions") version "0.60.6"
}