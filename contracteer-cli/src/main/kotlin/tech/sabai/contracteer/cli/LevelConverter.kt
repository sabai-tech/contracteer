package tech.sabai.contracteer.cli

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Level.INFO
import ch.qos.logback.classic.Level.toLevel
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory
import picocli.CommandLine.ITypeConverter

class LevelConverter: ITypeConverter<Level> {
  override fun convert(value: String): Level = toLevel(value, INFO)

  companion object {
    fun configureLogging(level: Level) {
      val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
      rootLogger.level = level
    }
  }
}
