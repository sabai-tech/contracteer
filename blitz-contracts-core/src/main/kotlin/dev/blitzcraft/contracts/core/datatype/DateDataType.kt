package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.error
import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.success
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class DateDataType(isNullable: Boolean = false):
    DataType<String>("string/date", isNullable, String::class.java) {

  override fun doValidate(value: String) =
    try {
      LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
      success()
    } catch (e: Exception) {
      error("not a valid date")
    }

  override fun randomValue(): String {
    val year = Random.nextInt(2000, 2100)
    val month = Random.nextInt(1, 13)
    val day = Random.nextInt(1, LocalDate.of(year, month, 1).lengthOfMonth() + 1)

    return LocalDate.of(year, month, day).format(DateTimeFormatter.ISO_LOCAL_DATE)
  }
}