package tech.sabai.contracteer.cli

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Level.*
import picocli.CommandLine.Help.Ansi.AUTO
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import tech.sabai.contracteer.cli.LevelConverter.Companion.configureLogging
import tech.sabai.contracteer.cli.LevelConverter.Companion.enableHttpTrafficLogging
import tech.sabai.contracteer.core.operation.ApiOperation
import tech.sabai.contracteer.core.Result.Success
import tech.sabai.contracteer.core.swagger.OpenApiLoader
import java.util.concurrent.Callable
import kotlin.system.exitProcess

abstract class BaseCliCommand: Callable<Unit> {

  @Parameters(index = "0",
              description = ["Path or URL of the OpenAPI 3 Specification that defines the API operations."]
  )
  protected lateinit var path: String

  @Option(
    names = ["-l", "--log-level"],
    description = [$$"Specify the log verbosity. Options: TRACE, DEBUG, INFO, WARN, ERROR, OFF, ALL. Default: @|bold ${DEFAULT-VALUE}|@."],
    converter = [LevelConverter::class],
    defaultValue = "INFO"
  )
  protected var logLevel: Level = INFO

  @Option(
    names = ["-t", "--http-traffic"],
    description = ["Enable HTTP request/response logging."],
    defaultValue = "false"
  )
  private var httpTraffic: Boolean = false

  override fun call() {
    configureLogging(logLevel)
    if (httpTraffic) enableHttpTrafficLogging()
    runCommand()
  }

  protected abstract fun runCommand()

  protected fun loadOperations(path: String): List<ApiOperation> {
    val result = OpenApiLoader.loadOperations(path)
    if (result !is Success) {
      println(AUTO.string("@|bold,red   ❌ Error while loading Operations:|@"))
      result.errors().forEach { println(AUTO.string("     ↳ @|yellow $it|@")) }
      exitProcess(1)
    }

    return result.value
  }
}