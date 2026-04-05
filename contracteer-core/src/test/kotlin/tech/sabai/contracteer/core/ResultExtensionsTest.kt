package tech.sabai.contracteer.core

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success

class ResultExtensionsTest {

  // -- Collection.accumulate --

  @Test
  fun `accumulate succeeds when all transforms succeed`() {
    // when
    val result = listOf(1, 2, 3).accumulate { success(it) }

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `accumulate accumulates all errors when multiple transforms fail`() {
    // when
    val result = listOf("a", "b", "c").accumulate { failure<String>(it, "invalid") }

    // then
    val errors = result.assertFailure()
    assert(errors.size == 3)
    assert(errors == listOf("'a': invalid", "'b': invalid", "'c': invalid"))
  }

  // -- Map.accumulate --

  @Test
  fun `map accumulate returns map of transformed values on success`() {
    // when
    val result = mapOf("a" to 1, "b" to 2).accumulate { (_, v) -> success(v * 2) }

    // then
    val value = result.assertSuccess()
    assert(value == mapOf("a" to 2, "b" to 4))
  }

  @Test
  fun `map accumulate accumulates all errors when multiple transforms fail`() {
    // when
    val result = mapOf("a" to 1, "b" to 2).accumulate { (k, _) -> failure<Int>(k, "invalid") }

    // then
    val errors = result.assertFailure()
    assert(errors.size == 2)
    assert(errors.contains("'a': invalid"))
    assert(errors.contains("'b': invalid"))
  }

  // -- List.accumulateWithIndex --

  @Test
  fun `accumulateWithIndex returns list of transformed values on success`() {
    // when
    val result = listOf("a", "b", "c").accumulateWithIndex { _, element -> success(element.uppercase()) }

    // then
    val value = result.assertSuccess()
    assert(value == listOf("A", "B", "C"))
  }

  @Test
  fun `accumulateWithIndex accumulates errors with index from multiple failures`() {
    // when
    val result = listOf("a", "b").accumulateWithIndex { index, _ -> failure<String>(index, "invalid") }

    // then
    val errors = result.assertFailure()
    assert(errors.size == 2)
    assert(errors == listOf("'[0]': invalid", "'[1]': invalid"))
  }

  // -- Collection<Result>.combineResults --

  @Test
  fun `combineResults returns list of values when all succeed`() {
    // when
    val result = listOf(success(1), success(2), success(3)).combineResults()

    // then
    val value = result.assertSuccess()
    assert(value == listOf(1, 2, 3))
  }

  @Test
  fun `combineResults accumulates errors from multiple failures`() {
    // when
    val result = listOf(failure<Int>("error 1"), success(1), failure<Int>("error 2")).combineResults()

    // then
    val errors = result.assertFailure()
    assert(errors == listOf("error 1", "error 2"))
  }

  @Test
  fun `combineResults includes null values when type is nullable`() {
    // when
    val result = listOf(success(1), success<Int?>(null), success(3)).combineResults()

    // then
    val value = result.assertSuccess()
    assert(value == listOf(1, null, 3))
  }
}