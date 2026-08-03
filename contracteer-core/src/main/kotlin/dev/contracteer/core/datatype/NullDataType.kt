package dev.contracteer.core.datatype

import dev.contracteer.core.Result
import dev.contracteer.core.datatype.GenerationOutcome.Value

/**
 * OAS 3.1 / JSON Schema `null` type — only the null value is valid.
 *
 * Singleton because the type carries no state and no per-schema configuration. The
 * [dataTypeClass] is [Unit], a sentinel that no JSON-parsed value matches; the base
 * class's null-shortcut handles legitimate null inputs and rejects every non-null
 * input with the standard *"Type mismatch, expected type 'null'"* failure.
 */
object NullDataType: ResolvedDataType<Unit>("null", "null", true, Unit::class.java, null) {

  override fun doValidate(value: Unit): Result<Unit> =
    error("unreachable: ResolvedDataType.validate handles every null and non-null case for NullDataType before doValidate")

  override fun doRandomValue(ctx: GenerationContext): GenerationOutcome<Unit> = Value(null)
}
