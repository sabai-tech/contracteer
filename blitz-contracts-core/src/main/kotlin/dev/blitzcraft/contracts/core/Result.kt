package dev.blitzcraft.contracts.core

import dev.blitzcraft.contracts.core.Result.Companion.success

class Result<out T> private constructor(
  val value: T? = null,
  private val propertyErrors: List<PropertyError> = emptyList()) {

  fun isSuccess() =
    propertyErrors.isEmpty()

  fun isFailure() =
    isSuccess().not()

  fun forProperty(propertyName: String) =
    Result(value, propertyErrors.map { it.prependProperty(propertyName) })

  fun forIndex(index: Int) =
    Result(value, propertyErrors.map { it.prependIndex(index) })

  fun errors() =
    propertyErrors.map { it.errorMessage() }

  fun <R> map(transform: (T) -> R): Result<R> =
    when {
      isSuccess() -> Result(value?.let { transform(it) })
      else        -> Result(propertyErrors = propertyErrors)
    }

  infix fun <R> combineWith(other: Result<R>): Result<Any?> =
    when {
      isSuccess() && other.isSuccess() -> success()
      else                             -> Result(propertyErrors = propertyErrors + other.propertyErrors)
    }

  companion object {
    fun <T> success(value: T? = null): Result<T> =
      Result(value)

    fun <T> failure(vararg errors: String): Result<T> =
      Result(propertyErrors = errors.map { PropertyError(error = it) })

    fun <T> failure(propertyIndex: Int, error: String): Result<T> =
      Result(propertyErrors = listOf(PropertyError(propertyIndex, error)))

    fun <T> failure(propertyName: String, error: String): Result<T> =
      Result(propertyErrors = listOf(PropertyError(propertyName, error)))
  }
}

fun <E> Collection<E>.accumulate(transform: (E) -> Result<Any?>): Result<Any?> =
  fold(success()) { acc, element -> acc combineWith transform(element) }

fun <K, V> Map<out K, V>.accumulate(transform: (Map.Entry<K, V>) -> Result<Any?>): Result<Any?> =
  entries.fold(success()) { acc, entry -> acc combineWith transform(entry) }

fun <E> Array<E>.accumulate(transform: (index: Int, E) -> Result<Any?>): Result<Any?> =
  indices.fold(success()) { acc, index -> acc.combineWith(transform(index, this[index])) }

private class PropertyError(val path: String = "", val error: String) {
  constructor(index: Int, error: String): this("[$index]", error)

  fun prependProperty(propertyName: String) =
    PropertyError(
      when {
        path.isEmpty()       -> propertyName
        path.startsWith("[") -> "$propertyName${path}"
        else                 -> "$propertyName.${path}"
      },
      error)

  fun prependIndex(index: Int) = prependProperty("[$index]")

  fun errorMessage() = if (path.isEmpty()) error else "'$path': $error"
}
