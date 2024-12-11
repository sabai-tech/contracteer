package dev.blitzcraft.contracts.verifier

import dev.blitzcraft.contracts.core.contract.Contract
import dev.blitzcraft.contracts.core.loader.loadOpenApiSpec
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
    val exitCode: Int
    val loadingResult = specFile.loadOpenApiSpec()
    if (loadingResult.hasErrors()) {
      println(AUTO.string("@|bold,red Invalid file:|@"))
      loadingResult.errors.forEach { println(AUTO.string("     - @|yellow $it|@")) }
      exitCode = 2
    } else {
      exitCode = runContractTests(loadingResult.contracts)
    }
    return exitCode
  }

  private fun runContractTests(contracts: Set<Contract>): Int {
    var exitCode = 0
    val serverVerifier = ServerVerifier(serverUrl, serverPort)
    println()
    println(AUTO.string("=== Verify @|bold,green $serverUrl:$serverPort|@ with @|bold '${specFile.name}'|@ ==="))
    contracts.forEach { contract ->
      print("* Validating ${contract.description()}: ")
      val validationResult = serverVerifier.verify(contract)
      if (validationResult.isSuccess()) println(AUTO.string("@|bold,green SUCCESS|@"))
      else {
        println(AUTO.string("@|bold,red ERROR|@"))
        validationResult.errors().forEach { println(AUTO.string("    - @|yellow $it|@")) }
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
