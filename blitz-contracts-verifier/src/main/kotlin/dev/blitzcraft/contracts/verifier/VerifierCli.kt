package dev.blitzcraft.contracts.verifier

import dev.blitzcraft.contracts.core.contract.Contract
import dev.blitzcraft.contracts.core.loader.swagger.generateContracts
import picocli.CommandLine
import picocli.CommandLine.*
import picocli.CommandLine.Help.Ansi.AUTO
import java.io.File
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Command(name = "server-verifier",
         mixinStandardHelpOptions = true,
         version = ["0.0.1"],
         description = ["validates server responses against a given OpenAPI 3 specifications."])
class VerifierCli: Callable<Int> {

  @Option(
    names = ["--server-url"],
    required = false,
    description = ["Server Url (default: \${DEFAULT-VALUE})"])
  private var serverUrl = "http://localhost"

  @Option(
    names = ["--server-port"],
    required = false,
    description = ["Server port (default: \${DEFAULT-VALUE})"])
  private var serverPort = 8080

  @Parameters(index = "0", description = ["Path of the Open Api 3 file "])
  lateinit var specFile: File

  override fun call(): Int {
    val result = specFile.generateContracts()
    return when  {
      result.isSuccess() -> runContractTests(result.value!!)
      else               -> printErrors(result.errors())
    }
  }

  private fun printErrors(errors: List<String>): Int {
    println(AUTO.string("@|bold,red Invalid file:|@"))
    errors.forEach { println(AUTO.string("     - @|yellow $it|@")) }
    return 2
  }

  private fun runContractTests(contracts: List<Contract>): Int {
    var exitCode = 0
    val serverVerifier = ServerVerifier(serverUrl, serverPort)
    println()
    println(AUTO.string("=== Verify @|bold,green $serverUrl:$serverPort|@ with @|bold '${specFile.name}'|@ ==="))
    contracts.forEach { contract ->
      print("* Validating ${contract.description()}: ")
      val testResult = serverVerifier.verify(contract)
      if (testResult.isSuccess()) println(AUTO.string("@|bold,green SUCCESS|@"))
      else {
        println(AUTO.string("@|bold,red ERROR|@"))
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
