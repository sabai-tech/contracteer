package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.accumulate
import tech.sabai.contracteer.core.normalize

class AllowedValues private constructor(values: List<Any?>) {
  private val allowedValues = values.distinct().map { it.normalize() }

  fun contains(value: Any?) =
    if (allowedValues.contains(value.normalize())) success(value)
    else failure("value '${value.formatValue()}' is not allowed. Possible values: ${allowedValues.formatValue()}")

  fun randomValue(): Any = allowedValues.filterNotNull().random()

  companion object {
    fun <T, DT: DataType<T>> create(values: List<Any?>, dataType: DT) =
      when {
        values.isEmpty()                              -> failure("'enum' cannot be empty")
        values.contains(null) && !dataType.isNullable -> failure("'enum' cannot contain a null value when schema is not nullable")
        else                                          ->
          values.accumulate { dataType.validate(it) }.forProperty("enum").map { AllowedValues(values) }
      }
  }

  private fun Any?.formatValue(): String =
    when (this) {
      is Collection<*> -> this.joinToString(prefix = "[", postfix = "]") { it.formatValue() }
      else             -> this.toString()
    }
}

