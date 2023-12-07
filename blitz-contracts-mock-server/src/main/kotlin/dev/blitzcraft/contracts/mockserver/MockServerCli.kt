package dev.blitzcraft.contracts.mockserver

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import dev.blitzcraft.contracts.core.ContractExtractor

class MockServerCli: CliktCommand() {
  private val port by option().int().default(8080).help("Port. Default is 8080")
  private val specFile by option()
    .file(mustExist = true, canBeDir = false)
    .required()
    .help("Path of the Open Api Spec file ")

  override fun run() {
    val mockServer = MockServer(port, ContractExtractor.extractFrom(specFile.toPath()))
    mockServer.start()
  }
}

fun main(args: Array<String>) = MockServerCli().main(args)