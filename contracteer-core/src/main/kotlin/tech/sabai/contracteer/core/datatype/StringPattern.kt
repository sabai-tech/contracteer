package tech.sabai.contracteer.core.datatype

import com.github.curiousoddman.rgxgen.RgxGen
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success

/**
 * Encapsulates an OpenAPI `pattern` value end-to-end: validation of string values against
 * the pattern, and generation of random values guaranteed to match it.
 *
 * Validation uses the original pattern compiled as a Java [Regex]. Generation uses
 * [RgxGen], possibly fed a rewritten pattern so RgxGen can handle constructs it would
 * otherwise fail on. Creation performs a sample-based check to ensure the generator
 * actually produces values matching the original pattern; patterns that parse successfully
 * but silently corrupt their output are rejected at creation time.
 */
internal class StringPattern private constructor(
  val source: String,
  private val compiledRegex: Regex,
  private val generator: RgxGen
) {

  /** Validates that [value] matches the pattern. Uses `containsMatchIn` semantics. */
  fun validate(value: String): Result<String> =
    if (compiledRegex.containsMatchIn(value)) success(value)
    else failure("Value '$value' does not match pattern '$source'.")

  /** Generates a random value guaranteed to match the pattern (verified at creation time). */
  fun randomValue(): String = generator.generate()

  companion object {
    private const val SAMPLE_COUNT = 50

    fun create(pattern: String) =
      compileJavaRegex(pattern).flatMap { compiledRegex ->
        parseRgxGen(pattern).flatMap { generator ->
          verifySamples(pattern, compiledRegex, generator)
        }
      }

    private fun compileJavaRegex(pattern: String): Result<Regex> =
      try {
        success(Regex(pattern))
      } catch (e: Throwable) {
        failure("'pattern' is not a valid regular expression (ECMA-262 / Java regex): $pattern (${shortCause(e)})")
      }

    private fun parseRgxGen(pattern: String): Result<RgxGen> =
      try {
        success(RgxGen.parse(PatternRewriter.rewrite(pattern)))
      } catch (e: Throwable) {
        failure("'pattern' uses constructs not supported by the random value generator: $pattern (${shortCause(e)})")
      }

    private fun verifySamples(pattern: String, compiledRegex: Regex, generator: RgxGen): Result<StringPattern> =
      generateSamples(generator, pattern).flatMap { samples ->
        try {
          val firstBad = samples.firstOrNull { !compiledRegex.containsMatchIn(it) }
          if (firstBad != null) silentCorruptionFailure(pattern, firstBad)
          else success(StringPattern(pattern, compiledRegex, generator))
        } catch (_: StackOverflowError) {
          failure("'pattern' is too complex for the regex engine: $pattern")
        }
      }

    private fun generateSamples(generator: RgxGen, pattern: String): Result<List<String>> =
      runCatching {
        (1..SAMPLE_COUNT).map { generator.generate() }
      }.fold(
        onSuccess = { success(it) },
        onFailure = { e -> failure("'pattern' value generation failed: $pattern (${shortCause(e)})") }
      )

    private fun silentCorruptionFailure(pattern: String, sample: String): Result<StringPattern> =
      failure("'pattern' value generation produces values that do not match the pattern: $pattern (e.g., '${sample.take(40)}')")

    private fun shortCause(e: Throwable): String =
      e.message?.lines()?.firstOrNull() ?: e::class.simpleName.orEmpty()
  }
}