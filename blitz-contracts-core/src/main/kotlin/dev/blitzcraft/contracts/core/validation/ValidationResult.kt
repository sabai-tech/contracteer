package dev.blitzcraft.contracts.core.validation

import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.success

sealed interface ValidationResult {
  fun isSuccess(): Boolean
  fun forProperty(propertyName: String): ValidationResult
  fun forIndex(index: Int) = forProperty("[$index]")
  fun errors(): List<String>
  infix fun and(other: ValidationResult): ValidationResult

  companion object {
    fun success(): ValidationResult =
      SimpleValidationResult()

    fun error(error: String): ValidationResult =
      SimpleValidationResult(emptyList(), error)

    fun error(propertyIndex: Int, error: String): ValidationResult =
      SimpleValidationResult(listOf("[$propertyIndex]"), error)

    fun error(propertyName: String, error: String): ValidationResult =
      SimpleValidationResult(listOf(propertyName), error)
  }
}

fun <T> Collection<T>.validate(transform: (T) -> ValidationResult): ValidationResult =
  map(transform).reduce()

fun <K, V> Map<out K, V>.validate(transform: (Map.Entry<K, V>) -> ValidationResult): ValidationResult =
  map(transform).reduce()

fun <T> Array<T>.validate(transform: (index: Int, T) -> ValidationResult): ValidationResult =
  mapIndexed(transform).reduce()

fun Collection<ValidationResult>.reduce(): ValidationResult =
  if (isEmpty()) success() else CompositeValidationResult(this)


private data class SimpleValidationResult(
  private val propertyPath: List<String> = emptyList(),
  private val error: String? = null): ValidationResult {

  override fun isSuccess() = error == null

  override fun forProperty(propertyName: String) =
    SimpleValidationResult(listOf(propertyName) + propertyPath, error)

  override fun errors() =
    when {
      error == null          -> listOf()
      propertyPath.isEmpty() -> listOf(error)
      else                   -> listOf("${propertyPathAsString()}: $error")
    }

  private fun propertyPathAsString() =
    propertyPath.fold("") { acc, name ->
      when {
        acc.isEmpty()        -> name
        name.startsWith("[") -> "$acc$name"
        else                 -> "$acc.$name"
      }
    }

  override fun and(other: ValidationResult) =
    if (other is SimpleValidationResult) CompositeValidationResult(listOf(this, other))
    else other and this
}

private data class CompositeValidationResult(
  private val validationResults: Collection<ValidationResult>): ValidationResult {

  override fun isSuccess() =
    validationResults.all { it.isSuccess() }


  override fun forProperty(propertyName: String) =
    CompositeValidationResult(validationResults.map { it.forProperty(propertyName) })

  override fun errors() =
    validationResults.filterNot { it.isSuccess() }.flatMap { it.errors() }

  override fun and(other: ValidationResult) =
    CompositeValidationResult(validationResults + other)
}