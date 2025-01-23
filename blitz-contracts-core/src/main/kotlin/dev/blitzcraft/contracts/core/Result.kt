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

  fun <R> map(transform: (T?) -> R): Result<R> =
    if (isSuccess()) success(transform(value))
    else this.retypeError()

  fun <R> flatMap(transform: (T?) -> Result<R>): Result<R> =
    if (isSuccess()) transform(value)
    else this.retypeError()

  fun mapErrors(transform: (String) -> String): Result<T> =
    if (isSuccess()) this
    else Result(propertyErrors = propertyErrors.map { PropertyError("", transform(it.errorMessage())) })

  @Suppress("UNCHECKED_CAST")
  fun <R> retypeError(): Result<R> =
    if (isSuccess()) success() else this as Result<R>

  infix fun combineWith(other: Result<@UnsafeVariance T>): Result<T> =
    if (isSuccess() && other.isSuccess()) success()
    else Result(propertyErrors = propertyErrors + other.propertyErrors)

  infix fun <E> andThen(next: () -> Result<E>): Result<E> =
    if (isFailure()) Result(propertyErrors = propertyErrors + next().propertyErrors)
    else next()

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

  private class PropertyError(val path: String = "", val error: String) {
    constructor(index: Int, error: String): this("[$index]", error)

    fun prependProperty(propertyName: String) =
      PropertyError(buildPath(propertyName), error)

    fun prependIndex(index: Int) = prependProperty("[$index]")

    fun errorMessage() = if (path.isEmpty()) error else "'$path': $error"

    private fun buildPath(propertyName: String) = when {
      path.isEmpty()       -> propertyName
      path.startsWith("[") -> "$propertyName${path}"
      else                 -> "$propertyName.${path}"
    }
  }
}

fun <E, R> Collection<E>.accumulate(transform: (E) -> Result<R>): Result<R> =
  fold(success()) { acc, element -> acc combineWith transform(element) }

fun <K, V, R> Map<out K, V>.accumulate(transform: (Map.Entry<K, V>) -> Result<R>): Result<Map<K, R>> =
  entries.fold(success(emptyMap())) { acc, entry ->
    val result = transform(entry)
    when {
      result.isSuccess() and acc.isSuccess() -> success(acc.value!! + (entry.key to result.value!!))
      result.isSuccess() and acc.isFailure() -> acc
      result.isFailure() and acc.isSuccess() -> result.retypeError()
      else                                   -> acc combineWith result.retypeError()
    }
  }

inline fun <E, reified R> Array<E>.accumulate(transform: (index: Int, E) -> Result<R>): Result<Array<R>> =
  indices.fold(success(emptyList<R>())) { acc, index ->
    val result = transform(index, this[index])
    when {
      result.isSuccess() && acc.isSuccess() -> success(acc.value!! + result.value!!)
      result.isSuccess() && acc.isFailure() -> acc
      result.isFailure() && acc.isSuccess() -> result.retypeError()
      else                                  -> acc combineWith result.retypeError()
    }
  }.let {
    if (it.isSuccess()) success(it.value!!.toTypedArray())
    else it.retypeError()
  }

fun <E> Collection<Result<E>>.combineResults(): Result<List<E>> =
  fold(success(emptyList())) { acc, current ->
    if (acc.isSuccess() and current.isSuccess()) success((acc.value ?: emptyList()) + listOfNotNull(current.value))
    else acc combineWith current.retypeError()
  }
