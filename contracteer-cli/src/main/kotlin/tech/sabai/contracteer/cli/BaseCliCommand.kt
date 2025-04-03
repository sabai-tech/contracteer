package tech.sabai.contracteer.cli

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Level.*
import picocli.CommandLine.Help.Ansi.AUTO
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import tech.sabai.contracteer.cli.LevelConverter.Companion.configureLogging
import tech.sabai.contracteer.core.contract.Contract
import tech.sabai.contracteer.core.swagger.OpenApiLoader
import java.util.concurrent.Callable
import kotlin.system.exitProcess

abstract class BaseCliCommand: Callable<Unit> {

  @Parameters(index = "0",
              description = ["Path or URL of the OpenAPI 3 Specification that defines the API contracts."]
  )
  protected lateinit var path: String

  @Option(
    names = ["-l", "--log-level"],
    description = ["Specify the log verbosity. Options: TRACE, DEBUG, INFO, WARN, ERROR, OFF, ALL. Default: @|bold \${DEFAULT-VALUE}|@."],
    converter = [LevelConverter::class],
    defaultValue = "INFO"
  )
  protected var logLevel: Level = INFO

  override fun call() {
    configureLogging(logLevel)
    return runCommand()
  }

  protected abstract fun runCommand()

  protected fun loadContracts(path: String): List<Contract> {
    val result = OpenApiLoader.loadContracts(path)
    if (result.isFailure()) {
      println(AUTO.string("@|bold,red   ❌ Error while generating Contracts:|@"))
      result.errors().forEach { println(AUTO.string("     ↳ @|yellow $it|@")) }
      exitProcess(1)
    }

    return result.value!!
  }
}