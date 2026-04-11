package tech.sabai.contracteer.core.datatype

/**
 * Rewrites a Java regex pattern into an equivalent or subset form that the value generator
 * ([com.github.curiousoddman.rgxgen.RgxGen]) can parse without its known parse-time failures.
 *
 * Used only for value generation; validation continues to use the original pattern so that
 * accepted values are never narrower than the spec author declared.
 *
 * Rewrite rules fall into two categories:
 *
 *   - **Lossless**: the rewrite is a strict equivalence under Java regex semantics
 *     (for example, [\p{IsLetter}] → [\p{L}], or moving a literal dash to a safe position
 *     inside a character class). Generated values match the same set as the original.
 *
 *   - **Subset**: the rewrite is a strict subset of the original pattern's matching set
 *     (for example, [\p{Print}] → [[\x20-\x7E]], an ASCII subset of Unicode printable
 *     characters). Generated values are still valid per the original pattern; the
 *     generator simply explores fewer of them.
 */
internal object PatternRewriter {

  private val literalReplacements: Map<String, String> = mapOf(
    // Java `Is` aliases → standard Unicode short form or ASCII subset
    "\\p{IsLetter}" to "\\p{L}",
    "\\p{IsDigit}" to "[0-9]",
    "\\p{IsAlphabetic}" to "[a-zA-Z]",
    "\\p{IsLowercase}" to "[a-z]",
    "\\p{IsUppercase}" to "[A-Z]",
    "\\p{IsWhiteSpace}" to "[ \\t\\n\\r\\f\\x0B]",

    // POSIX classes → ASCII character class subsets
    "\\p{Alpha}" to "[a-zA-Z]",
    "\\p{Alnum}" to "[a-zA-Z0-9]",
    "\\p{ASCII}" to "[\\x00-\\x7F]",
    "\\p{Blank}" to "[ \\t]",
    "\\p{Cntrl}" to "[\\x00-\\x1F\\x7F]",
    "\\p{Digit}" to "[0-9]",
    "\\p{Graph}" to "[\\x21-\\x7E]",
    "\\p{Lower}" to "[a-z]",
    "\\p{Print}" to "[\\x20-\\x7E]",
    "\\p{Punct}" to "[!-/:-@\\[-`{-~]",
    "\\p{Space}" to "[ \\t\\n\\r\\f\\x0B]",
    "\\p{Upper}" to "[A-Z]",
    "\\p{XDigit}" to "[0-9A-Fa-f]",

    // Java `java…` method classes → ASCII equivalents
    "\\p{javaLowerCase}" to "[a-z]",
    "\\p{javaUpperCase}" to "[A-Z]",
    "\\p{javaWhitespace}" to "[ \\t\\n\\r\\f\\x0B]",

    // Unicode "Other" general category — RgxGen NPE on `\p{C}`/`\P{C}` specifically
    "\\p{C}" to "[\\x00-\\x1F\\x7F]",
    "\\P{C}" to "[\\p{L}\\p{N}\\p{P}\\p{S}\\p{M}\\p{Z}]"
  )

  private val charClassInPattern = Regex("""\[([^\[\]]*)]""")
  private val dashBeforeShorthand = Regex("""-(\\[sSwWdD])""")

  /** Applies all known rewrite rules in sequence to [pattern] and returns the result. */
  fun rewrite(pattern: String): String {
    val afterLiterals = literalReplacements.entries.fold(pattern) { acc, (from, to) -> acc.replace(from, to) }

    return rewriteDashPositionInCharClass(afterLiterals)
  }

  /**
   * Inside any character class, strips every literal dash that appears before a shorthand
   * escape (`\s`, `\S`, `\w`, `\W`, `\d`, `\D`) and, if any was stripped, appends a single
   * literal `-` at the end of the class. The result is semantically equivalent to the
   * original: every stripped dash was already a literal (a shorthand cannot be a range
   * endpoint), and the appended dash is also literal. RgxGen rejects dash-before-shorthand
   * as a malformed range; the rewritten form sidesteps that.
   *
   * Limitation: character classes that contain escaped brackets (e.g., `[a-z\[\]-\s]`) are
   * not rewritten because the outer match stops at any bracket character.
   */
  private fun rewriteDashPositionInCharClass(pattern: String): String =
    charClassInPattern.replace(pattern) { match ->
      val content = match.groupValues[1]
      if (!dashBeforeShorthand.containsMatchIn(content)) match.value
      else {
        val stripped = content.replace(dashBeforeShorthand, "$1")
        val suffix = if (stripped.endsWith("-")) "" else "-"
        "[$stripped$suffix]"
      }
    }
}