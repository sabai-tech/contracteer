package dev.blitzcraft.contracts.verifier

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import dev.blitzcraft.contracts.core.ContractExtractor

class VerifierCli: CliktCommand() {
  private val serverPort by option().int().default(8080).help("Server port. Default is 8080")
  private val serverUrl by option().default("http://localhost").help("Server Url. Default is http://localhost")
  private val specFile by option()
    .file(mustExist = true, canBeDir = false)
    .required()
    .help("Path of the Open Api Spec file ")

  override fun run() {
    val serverVerifier = ServerVerifier(serverUrl, serverPort)
    ContractExtractor.extractFrom(specFile.toPath()).forEach {
      echo("Validate ${it.description()}")
      serverVerifier.verify(it)
    }
  }
}

fun main(args: Array<String>) = VerifierCli().main(args)