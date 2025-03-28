package tech.sabai.contracteer.mockserver

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Level.*
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory
import picocli.CommandLine
import picocli.CommandLine.*
import tech.sabai.contracteer.core.swagger.OpenApiLoader
import java.util.concurrent.Callable

@Command(name = "server-verifier",
         mixinStandardHelpOptions = true,
         version = ["0.0.1"],
         description = [
           "Starts a Contracteer Mock Server based on an OpenAPI 3 Definition."
         ]
)
class MockServerCli: Callable<Int> {

  @Parameters(index = "0",
              description = [
                "Specifies the location of an OpenAPI 3 specification,",
                "either as a local file path or a remote URL."
              ])
  lateinit var path: String

  @Option(
    names = ["-p", "--port"],
    required = false,
    description = ["Port to run Contracteer Mock Server (default: \${DEFAULT-VALUE})"])
  private var port = 8080

  @Option(
    names = ["-l", "--log-level"],
    description = ["Set the log level. Available values: TRACE, DEBUG, INFO, WARN, ERROR, OFF, ALL. Default: \${DEFAULT-VALUE}"],
    converter = [LevelConverter::class],
    defaultValue = "INFO"
  )
  private var logLevel: Level = INFO

  override fun call(): Int {
    var exitCode = 0
    configureLogging(logLevel)
    val result = OpenApiLoader.loadContracts(path)
    if (result.isFailure()) {
      println(Help.Ansi.AUTO.string("@|bold,red ❌ Failed to load OpenAPI Definition:|@"))
      result.errors().forEach { println(Help.Ansi.AUTO.string("     - @|yellow $it|@")) }
      exitCode = 1
    } else {
      val mockServer = MockServer(result.value!!, port)
      mockServer.start()
    }
    return exitCode
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
    fun main(args: Array<String>) {
      CommandLine(MockServerCli()).execute(*args)
    }
  }
}