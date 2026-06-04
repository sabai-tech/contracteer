package tech.sabai.contracteer.core.swagger.datatype

import io.swagger.v3.oas.models.media.Schema
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.NullDataType

/**
 * Rejects compositions where the outer schema's `type` array constrains the value to a
 * non-null type yet a sub-branch declares `type: null`. The two constraints cannot both
 * hold — either the outer must allow null too, or the null branch is meaningless.
 */
internal fun Schema<*>.rejectNullBranchAgainstOuterType(
  subTypes: List<DataType<out Any>>,
  compositionKeyword: String
): Result<Unit> {
  val outerTypes = types
  if (outerTypes.isNullOrEmpty() || "null" in outerTypes) return success()
  val nullBranchIndex = subTypes.indexOfFirst { it is NullDataType }
  if (nullBranchIndex < 0) return success()
  val formattedTypes = outerTypes.joinToString(prefix = "[", postfix = "]")
  return failure(
    "Schema '$name': outer 'type: $formattedTypes' constrains type but the '$compositionKeyword' branch '#$nullBranchIndex' " +
    "declares 'type: null'. Either add 'null' to the outer type array or remove the null branch.")
}

/**
 * Rejects `allOf` compositions that include a `type: null` branch. Such a branch is
 * unsatisfiable: the value must match every branch, which means it must be null AND
 * match every other (non-null) branch.
 */
internal fun Schema<*>.rejectAllOfNullBranch(subTypes: List<DataType<out Any>>): Result<Unit> {
  val nullBranchIndex = subTypes.indexOfFirst { it is NullDataType }
  if (nullBranchIndex < 0) return success()
  return failure(
    "Schema '$name': 'allOf' includes a 'type: null' branch (#$nullBranchIndex) " +
    "which is unsatisfiable: a value cannot match both the null type and any non-null branch. " +
    "Use 'anyOf' if a nullable union is intended, or remove the null branch.")
}
