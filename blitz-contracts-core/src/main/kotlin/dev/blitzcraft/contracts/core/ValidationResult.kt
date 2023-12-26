package dev.blitzcraft.contracts.core

sealed interface ValidationResult {
  fun isSuccess(): Boolean
  fun forProperty(propertyName: String): ValidationResult
  fun errors(): List<String>
}

data class SimpleValidationResult(private val propertyName: String,
                                  private val error: String? = null): ValidationResult {
  constructor(error: String): this("", error)
  constructor(): this("", null)

  override fun isSuccess() = error == null

  override fun forProperty(propertyName: String) = SimpleValidationResult(propertyName, error)

  override fun errors() = when {
    error == null          -> listOf()
    propertyName.isEmpty() -> listOf(error)
    else                   -> listOf("$propertyName: $error")
  }
}

data class CompositeValidationResult(private val propertyName: String,
                                     private val validationResults: List<ValidationResult>): ValidationResult {
  constructor(validationResults: List<ValidationResult>): this("", validationResults)

  override fun isSuccess() = validationResults.all { it.isSuccess() }

  override fun forProperty(propertyName: String) = CompositeValidationResult(propertyName, validationResults)

  override fun errors() =
    validationResults
      .filterNot { it.isSuccess() }
      .flatMap { it.errors().prependsWithPropertyName() }
  private fun List<String>.prependsWithPropertyName() = map {
    when {
      propertyName.isEmpty() -> it
      it.startsWith("[")     -> "$propertyName$it"
      else                   -> "$propertyName.$it"
    }
  }
}





