package tech.sabai.contracteer.core.serde

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import tech.sabai.contracteer.core.Mappers.jsonMapper
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.DataType

/** [Serde] implementation for `application/json` content. Uses Jackson for serialization and deserialization. */
object JsonSerde: Serde() {
  private val logger = KotlinLogging.logger {}
  private val objectMapper = ObjectMapper()

  override fun doSerialize(value: Any?): String =
    objectMapper.writeValueAsString(value)

  override fun doDeserialize(source: String?, targetDataType: DataType<out Any>): Result<Any?> =
    when {
      source.isNullOrBlank() -> success(null)
      source == "null"       -> success(null)
      else                   ->
        try {
          val success = success(jsonMapper.readValue(source, targetDataType.dataTypeClass))
          success
        } catch (e: Exception) {
          logger.debug { e }
          failure("Error while deserializing value. Exception: ${e.message}")
        }
    }
}
