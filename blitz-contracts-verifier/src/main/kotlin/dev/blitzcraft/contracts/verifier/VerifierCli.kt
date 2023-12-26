package dev.blitzcraft.contracts.verifier

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import dev.blitzcraft.contracts.core.ContractExtractor
import kotlin.system.exitProcess

class VerifierCli: CliktCommand() {
  private val serverPort by option().int().default(8080).help("Server port. Default is 8080")
  private val serverUrl by option().default("http://localhost").help("Server Url. Default is http://localhost")
  private val specFile by option()
    .file(mustExist = true, canBeDir = false)
    .required()
    .help("Path of the Open Api 3 file ")

  override fun run() {
    val serverVerifier = ServerVerifier(serverUrl, serverPort)
    var exitCode = 0
    echo()
    ContractExtractor.extractFrom(specFile.toPath()).forEach { contract ->
      echo("* Validating ${contract.description()}: ", trailingNewline = false)
      val validationResult = serverVerifier.verify(contract)
      if (validationResult.isSuccess()) echo("SUCCESS".inGreen())
      else {
        echo("ERROR".inRed())
        validationResult.errors().forEach { echo("   - $it".inYellow()) }
        exitCode = 2
      }
      echo()
    }
    exitProcess(exitCode)
  }
}

private fun String.inRed() = "\u001b[31m${this}\u001b[0m"
private fun String.inGreen() = "\u001b[32m${this}\u001b[0m"
private fun String.inYellow() = "\u001b[33m${this}\u001b[0m"

fun main(args: Array<String>) = VerifierCli().main(args)