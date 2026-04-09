package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/** OpenAPI `string` type with `format: date`. Values must conform to `YYYY-MM-DD`. */
class DateDataType private constructor(name: String, isNullable: Boolean, allowedValues: AllowedValues? = null):
    ResolvedDataType<String>(name, "string/date", isNullable, String::class.java, allowedValues) {

  override fun isFullyStructured() = false

  override fun doValidate(value: String) =
    try {
      LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
      success(value)
    } catch (_: Exception) {
      failure("Invalid date. The provided string is not in the 'format YYYY-MM-DD'")
    }

  override fun doRandomValue(): String {
    val year = Random.nextInt(2000, 2100)
    val month = Random.nextInt(1, 13)
    val day = Random.nextInt(1, LocalDate.of(year, month, 1).lengthOfMonth() + 1)

    return LocalDate.of(year, month, day).format(DateTimeFormatter.ISO_LOCAL_DATE)
  }

  companion object {
    @JvmStatic
    @JvmOverloads
    fun create(
      name: String,
      isNullable: Boolean = false,
      enum: List<String?> = emptyList()
    ) =
      DateDataType(name, isNullable).let { dataType ->
        if (enum.isEmpty()) success(dataType)
        else AllowedValues.create(enum, dataType).map { DateDataType(name, isNullable, it) }
      }
  }
}