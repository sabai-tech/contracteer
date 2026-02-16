@file:JvmName("Results")

package tech.sabai.contracteer.core

import tech.sabai.contracteer.core.Result.Companion.success

/**
 * Outcome of a validation or creation operation that is either a success carrying an optional [value],
 * or a failure carrying one or more property-scoped error messages.
 *
 * Errors are tracked with dotted property paths (e.g. `address.street`) so that validation messages
 * can pinpoint the exact location of a problem inside nested structures.
 */
class Result<out T> private constructor(
  val value: T? = null,
  private val propertyErrors: List<PropertyError> = emptyList()) {

  /** Returns `true` when this result carries no errors. */
  fun isSuccess() =
    propertyErrors.isEmpty()

  /** Returns `true` when this result carries at least one error. */
  fun isFailure() =
    isSuccess().not()

  /** Returns a new result with every error path prefixed by [propertyName]. */
  fun forProperty(propertyName: String) =
    Result(value, propertyErrors.map { it.prependProperty(propertyName) })

  /** Returns a new result with every error path prefixed by the array index `[index]`. */
  fun forIndex(index: Int) =
    Result(value, propertyErrors.map { it.prependIndex(index) })

  /** Returns the list of human-readable error messages, each prefixed with its property path. */
  fun errors() =
    propertyErrors.map { it.errorMessage() }

  /** Transforms the success value with [transform]; propagates errors unchanged on failure. */
  fun <R> map(transform: (T?) -> R): Result<R> =
    if (isSuccess()) success(transform(value))
    else this.retypeError()

  /** Transforms the success value with [transform] which itself returns a [Result]; propagates errors unchanged on failure. */
  fun <R> flatMap(transform: (T?) -> Result<R>): Result<R> =
    if (isSuccess()) transform(value)
    else this.retypeError()

  /** Transforms every error message with [transform]; returns this result unchanged on success. */
  fun mapErrors(transform: (String) -> String): Result<T> =
    if (isSuccess()) this
    else Result(propertyErrors = propertyErrors.map { PropertyError("", transform(it.errorMessage())) })

  /** Re-types a failure result to a different value type. Throws on success results that carry a non-null value. */
  @Suppress("UNCHECKED_CAST")
  fun <R> retypeError(): Result<R> =
    if (isSuccess()) success() else this as Result<R>

  /** Merges this result with [other], succeeding only if both succeed, and accumulating all errors otherwise. */
  infix fun combineWith(other: Result<@UnsafeVariance T>): Result<T> =
    if (isSuccess() && other.isSuccess()) success()
    else Result(propertyErrors = propertyErrors + other.propertyErrors)

  /** Chains a follow-up validation: on success, runs [next]; on failure, accumulates errors from both this result and [next]. */
  infix fun <E> andThen(next: () -> Result<E>): Result<E> =
    if (isFailure()) Result(propertyErrors = propertyErrors + next().propertyErrors)
    else next()

  override fun toString() =
    if (isSuccess()) "Result(success, value=${value})" else "Result(failure, errors=${errors()})"

  companion object {
    /** Creates a successful result, optionally carrying [value]. */
    @JvmStatic
    @JvmOverloads
    fun <T> success(value: T? = null): Result<T> =
      Result(value)

    /** Creates a failed result with the given error messages, not scoped to any property. */
    @JvmStatic
    fun <T> failure(vararg errors: String): Result<T> =
      Result(propertyErrors = errors.map { PropertyError(error = it) })

    /** Creates a failed result with [error] scoped to the array element at [propertyIndex]. */
    @JvmStatic
    fun <T> failure(propertyIndex: Int, error: String): Result<T> =
      Result(propertyErrors = listOf(PropertyError(propertyIndex, error)))

    /** Creates a failed result with [error] scoped to the property named [propertyName]. */
    @JvmStatic
    fun <T> failure(propertyName: String, error: String): Result<T> =
      Result(propertyErrors = listOf(PropertyError(propertyName, error)))
  }

  private class PropertyError(val path: String = "", val error: String) {
    constructor(index: Int, error: String): this("[$index]", error)

    fun prependProperty(propertyName: String) =
      PropertyError(buildPath(propertyName), error)

    fun prependIndex(index: Int) = prependProperty("[$index]")

    fun errorMessage() = if (path.isEmpty()) error else "'$path': $error"

    private fun buildPath(propertyName: String) =
      when {
        propertyName.isEmpty() -> path
        path.isEmpty()         -> propertyName
        path.startsWith("[")   -> "$propertyName${path}"
        else                   -> "$propertyName.${path}"
      }
  }
}

/** Applies [transform] to each element, accumulating all errors across the collection into a single result. */
fun <E, R> Collection<E>.accumulate(transform: (E) -> Result<R>): Result<R> =
  fold(success()) { acc, element -> acc combineWith transform(element) }

/** Applies [transform] to each entry, accumulating all errors. On success, returns a map of transformed values. */
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

/** Applies [transform] to each element with its index, accumulating all errors. On success, returns a list of transformed values. */
inline fun <E, reified R> List<E>.accumulateWithIndex(transform: (index: Int, E) -> Result<R>): Result<List<R>> =
  indices.fold(success(emptyList<R>())) { acc, index ->
    val result = transform(index, this[index])
    when {
      result.isSuccess() && acc.isSuccess() -> success(acc.value!! + result.value!!)
      result.isSuccess() && acc.isFailure() -> acc
      result.isFailure() && acc.isSuccess() -> result.retypeError()
      else                                  -> acc combineWith result.retypeError()
    }
  }.let {
    if (it.isSuccess()) success(it.value!!)
    else it.retypeError()
  }

/** Combines all results in this collection, succeeding with a list of values only if every result is a success. */
fun <E> Collection<Result<E>>.combineResults(): Result<List<E>> =
  fold(success(emptyList())) { acc, current ->
    if (acc.isSuccess() and current.isSuccess()) success((acc.value ?: emptyList()) + listOfNotNull(current.value))
    else acc combineWith current.retypeError()
  }
