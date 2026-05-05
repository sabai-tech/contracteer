package tech.sabai.contracteer.cli

import ch.qos.logback.classic.Level.DEBUG
import picocli.CommandLine.Command
import picocli.CommandLine.Help.Ansi.AUTO
import picocli.CommandLine.Option
import tech.sabai.contracteer.verifier.OpenApiVerifier
import tech.sabai.contracteer.verifier.VerificationCase
import tech.sabai.contracteer.verifier.VerificationCaseFactory
import tech.sabai.contracteer.verifier.VerificationOutcome
import tech.sabai.contracteer.verifier.VerifierConfiguration
import kotlin.system.exitProcess

@Command(
  name = "verify",
  synopsisHeading = "\n@|bold,cyan Usage|@:\n  ",
  descriptionHeading = "\n@|bold,cyan Description|@:\n  ",
  description = [
    "Verify that a server's API implementation adheres to the defined OpenAPI 3 Specification."
  ],
  optionListHeading = "\n@|bold,cyan Options|@:\n",
  parameterListHeading = "\n@|bold,cyan Parameters|@:\n",
  mixinStandardHelpOptions = true,
  usageHelpAutoWidth = true,
  abbreviateSynopsis = false
)
class VerifyCli: BaseCliCommand() {
  @Option(
    names = ["-u", "--base-url"],
    required = false,
    description = [$$"Base URL of the server to verify (must include scheme, host, and port). Default: @|bold ${DEFAULT-VALUE}|@."],
  )
  private var baseUrl = "http://localhost:8080"

  override fun runCommand() {
    val operations = loadOperations(path)
    val cases = operations.flatMap { VerificationCaseFactory.create(it) }
    runVerification(cases).also { exitProcess(it) }
  }

  private fun runVerification(cases: List<VerificationCase>): Int {
    val verifier = OpenApiVerifier(VerifierConfiguration(baseUrl))
    println()
    println(AUTO.string("🚀 Starting contract verification..."))
    println(AUTO.string("Target Server: @|bold,green $baseUrl|@"))
    println(AUTO.string("Specification: @|bold,green ${path}|@"))
    println()

    val outcomes = cases.map { verifier.verify(it).also { outcome -> printOutcome(outcome) } }

    println()
    println(AUTO.string("@|bold,blue Result Summary:|@"))
    val failures = outcomes.filter { it.result.isFailure() }
    if (failures.isNotEmpty()) {
      println(AUTO.string("   ⚠\uFE0F @|yellow ${failures.size}|@ errors found during verification."))
      println(AUTO.string("   ✅ @|yellow ${cases.size - failures.size}|@ verification cases passed."))
      return 1
    } else {
      println(AUTO.string("   \uD83C\uDF89 All ${outcomes.size} verification cases passed!"))
      return 0
    }
  }

  private fun printOutcome(outcome: VerificationOutcome) {
    if (logLevel == DEBUG) println()
    if (outcome.result.isSuccess()) {
      println(AUTO.string("   ✅ ${outcome.case.displayName}"))
    } else {
      println(AUTO.string("@|bold,red    ❌ ${outcome.case.displayName}|@"))
      outcome.result.errors().forEach { println(AUTO.string("     ↳ @|yellow $it|@")) }
    }

    if (logLevel == DEBUG) {
      println(AUTO.string("<===========================================================================================>"))
      println()
    }
  }
}
