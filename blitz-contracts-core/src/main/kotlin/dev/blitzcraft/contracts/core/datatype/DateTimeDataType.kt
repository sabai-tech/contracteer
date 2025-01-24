package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.Result.Companion.failure
import dev.blitzcraft.contracts.core.Result.Companion.success
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class DateTimeDataType(name: String= "Inline 'string/date-time' Schema", isNullable: Boolean = false):
    DataType<String>(name, "string/date-time", isNullable, String::class.java) {

  override fun doValidate(value: String) =
    try {
      OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      success(value)
    } catch (e: Exception) {
      failure("not a valid date-time")
    }

  override fun randomValue(): String {
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

}