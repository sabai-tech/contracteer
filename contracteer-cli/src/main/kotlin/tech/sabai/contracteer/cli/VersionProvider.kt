package tech.sabai.contracteer.cli

import picocli.CommandLine.IVersionProvider
import kotlin.io.readText
import kotlin.jvm.java
import kotlin.text.trim

class VersionProvider : IVersionProvider {

  override fun getVersion(): Array<String> =
    arrayOf(Cli::class.java.getResource("/version.txt")?.readText()?.trim() ?: "unknown")
}