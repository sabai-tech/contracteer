package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.Result.Companion.failure
import dev.blitzcraft.contracts.core.Result.Companion.success
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class DateDataType(name: String= "Inline Schema", isNullable: Boolean = false):
    DataType<String>(name, "string/date", isNullable, String::class.java) {

  override fun doValidate(value: String) =
    try {
      LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
      success(value)
    } catch (e: Exception) {
      failure("not a valid date")
    }

  override fun randomValue(): String {
    val year = Random.nextInt(2000, 2100)
    val month = Random.nextInt(1, 13)
    val day = Random.nextInt(1, LocalDate.of(year, month, 1).lengthOfMonth() + 1)

    return LocalDate.of(year, month, day).format(DateTimeFormatter.ISO_LOCAL_DATE)
  }
}