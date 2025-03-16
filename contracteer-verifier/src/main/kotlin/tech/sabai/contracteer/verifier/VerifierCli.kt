package tech.sabai.contracteer.verifier

import picocli.CommandLine
import picocli.CommandLine.*
import picocli.CommandLine.Help.Ansi.AUTO
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

  @Parameters(index = "0",
              description = [
                "Specifies the location of an OpenAPI 3 specification,",
                "either as a local file path or a remote URL."
              ]
  )
  lateinit var path: String

  override fun call(): Int {
    val result = OpenApiLoader.loadContracts(path)
    return when {
      result.isSuccess() -> runContractTests(result.value!!)
      else               -> printErrors(result.errors())
    }
  }

  private fun printErrors(errors: List<String>): Int {
    println(AUTO.string("@|bold,red ❌ Verification failed:|@"))
    errors.forEach { println(AUTO.string("     - @|yellow $it|@")) }
    return 2
  }

  private fun runContractTests(contracts: List<Contract>): Int {
    var exitCode = 0
    val serverVerifier = ServerVerifier(ServerConfiguration(serverUrl, serverPort))
    println()
    println(AUTO.string("=== Verify @|bold,green $serverUrl:$serverPort|@ with @|bold '${path}'|@ ==="))
    contracts.forEach { contract ->
      print("* Validating ${contract.description()}: ")
      val testResult = serverVerifier.verify(contract)
      if (testResult.isSuccess()) println(AUTO.string("@|bold,green ✅ SUCCESS|@"))
      else {
        println(AUTO.string("@|bold,red ❌ ERROR|@"))
        testResult.errors().forEach { println(AUTO.string("    - @|yellow $it|@")) }
        exitCode = 2
        println()
      }
    }
    return exitCode
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>): Unit = exitProcess(CommandLine(VerifierCli()).execute(*args))
  }
}
