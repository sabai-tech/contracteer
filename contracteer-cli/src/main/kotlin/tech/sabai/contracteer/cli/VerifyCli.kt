package tech.sabai.contracteer.cli

import ch.qos.logback.classic.Level.DEBUG
import picocli.CommandLine.Command
import picocli.CommandLine.Help.Ansi.AUTO
import picocli.CommandLine.Option
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.contract.Contract
import tech.sabai.contracteer.verifier.ServerConfiguration
import tech.sabai.contracteer.verifier.ServerVerifier
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
    names = ["-u", "--server-url"],
    required = false,
    description = ["Server's base URL to verify against. Default: @|bold \${DEFAULT-VALUE}|@."],
  )
  private var serverUrl = "http://localhost"

  @Option(
    names = ["-p", "--server-port"],
    required = false,
    description = ["Port number for the server to verify. Default: @|bold \${DEFAULT-VALUE}|@."]
  )
  private var serverPort = 8080

  override fun runCommand() {
    val result = loadContracts(path)
    runContractTests(result).also { exitProcess(it) }
  }

  private fun runContractTests(contracts: List<Contract>): Int {
    val serverVerifier = ServerVerifier(ServerConfiguration(serverUrl, serverPort))
    println()
    println(AUTO.string("🚀 Starting contract verification..."))
    println(AUTO.string("Target Server: @|bold,green $serverUrl:$serverPort|@"))
    println(AUTO.string("Specification: @|bold,green ${path}|@"))
    println()

    val results = contracts.map { contract ->
      contract to serverVerifier.verify(contract).also { printVerificationResult(contract, it) }
    }

    println()
    println(AUTO.string("@|bold,blue Result Summary:|@"))
    val failures = results.filter { it.second.isFailure() }
    if (failures.isNotEmpty()) {
      println(AUTO.string("   ⚠\uFE0F @|yellow ${failures.size}|@ errors found during verification."))
      println(AUTO.string("   ✅ @|yellow ${contracts.size - failures.size}|@ contracts successfully verified."))
      return 1
    } else {
      println(AUTO.string("   \uD83C\uDF89 All contracts successfully verified!"))
      return 0
    }
  }

  private fun printVerificationResult(contract: Contract, result: Result<Contract>) {
    if (logLevel == DEBUG) println()
    if (result.isSuccess()) {
      println(AUTO.string("   ✅ ${contract.description()}"))
    } else {
      println(AUTO.string("@|bold,red   ❌ ${contract.description()}|@"))
      result.errors().forEach { println(AUTO.string("     ↳ @|yellow $it|@")) }
    }

    if (logLevel == DEBUG) {
      println(AUTO.string("<===========================================================================================>"))
      println()
    }
  }
}
