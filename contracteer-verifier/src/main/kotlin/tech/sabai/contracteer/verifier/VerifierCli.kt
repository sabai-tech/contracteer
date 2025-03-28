package tech.sabai.contracteer.verifier

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Level.*
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory
import picocli.CommandLine
import picocli.CommandLine.*
import picocli.CommandLine.Help.Ansi.AUTO
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.contract.Contract
import tech.sabai.contracteer.core.swagger.OpenApiLoader
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Command(name = "Contracteer Verifier",
         mixinStandardHelpOptions = true,
         version = ["0.0.1"],
         description = [
           "Verifies that a server implementation conforms to an OpenAPI 3 specification"
         ])
class VerifierCli: Callable<Int> {

  @Parameters(index = "0",
              description = [
                "Specifies the location of an OpenAPI 3 specification,",
                "either as a local file path or a remote URL."
              ]
  )
  lateinit var path: String

  @Option(
    names = ["--server-url"],
    required = false,
    description = ["Base URL of the server to verify (default: \${DEFAULT-VALUE})"])
  private var serverUrl = "http://localhost"

  @Option(
    names = ["--server-port"],
    required = false,
    description = ["Server port (default: \${DEFAULT-VALUE})"])
  private var serverPort = 8080

  @Option(
    names = ["-l", "--log-level"],
    description = ["Set the log level. Available values: TRACE, DEBUG, INFO, WARN, ERROR, OFF, ALL. Default: \${DEFAULT-VALUE}"],
    converter = [LevelConverter::class],
    defaultValue = "INFO"
  )
  private var logLevel: Level = INFO

  override fun call(): Int {
    configureLogging(logLevel)
    val result = OpenApiLoader.loadContracts(path)
    return when {
      result.isSuccess() -> runContractTests(result.value!!)
      else               -> printErrors(result.errors())
    }
  }

  private fun printErrors(errors: List<String>): Int {
    println(AUTO.string("@|bold,red   ❌ Verification failed:|@"))
    errors.forEach { println(AUTO.string("     ↳ @|yellow $it|@")) }
    return 1
  }

  private fun runContractTests(contracts: List<Contract>): Int {
    val serverVerifier = ServerVerifier(ServerConfiguration(serverUrl, serverPort))
    println()
    println(AUTO.string("\uD83D\uDE80 Verifying Contracts:"))
    println(AUTO.string("Server: @|bold,green $serverUrl:$serverPort|@"))
    println(AUTO.string("Spec: @|bold,green ${path}|@"))
    println()

    val results = contracts.map { contract ->
      contract to serverVerifier.verify(contract).also { printVerificationResult(contract, it) }
    }

    println()
    println(AUTO.string("@|bold Result: |@"))
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

  private fun configureLogging(level: Level) {
    val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    rootLogger.level = level
  }

  class LevelConverter: ITypeConverter<Level> {
    override fun convert(value: String): Level = toLevel(value, INFO)
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>): Unit = exitProcess(CommandLine(VerifierCli()).execute(*args))
  }
}
