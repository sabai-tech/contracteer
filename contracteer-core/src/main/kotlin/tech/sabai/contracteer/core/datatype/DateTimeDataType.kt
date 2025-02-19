package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class DateTimeDataType private constructor(name: String, isNullable: Boolean, allowedValues: AllowedValues? = null):
    DataType<String>(name, "string/date-time", isNullable, String::class.java, allowedValues) {

  override fun doValidate(value: String) =
    try {
      OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      success(value)
    } catch (e: Exception) {
      failure("not a valid date-time")
    }

  override fun doRandomValue(): String {
    val year = Random.nextInt(2000, 2100)
    val month = Random.nextInt(1, 13)
    val day = Random.nextInt(1, LocalDate.of(year, month, 1).lengthOfMonth() + 1)
    val hour = Random.nextInt(0, 24)
    val minute = Random.nextInt(0, 60)
    val second = Random.nextInt(0, 60)
    val nano = Random.nextInt(0, 1_000_000_000)
    val offset = ZoneOffset.ofHours(Random.nextInt(-12, 15))
    val dateTime = OffsetDateTime.of(year, month, day, hour, minute, second, nano, offset)

    return dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
  }

  companion object {
    fun create(
      name: String = "Inline 'string/date-time' Schema",
      isNullable: Boolean = false,
      enum: List<Any?> = emptyList()
    ) =
      DateTimeDataType(name, isNullable).let { dataType ->
        if (enum.isEmpty()) success(dataType)
        else AllowedValues.create(enum, dataType).map { DateTimeDataType(name, isNullable, it) }
      }
  }
}