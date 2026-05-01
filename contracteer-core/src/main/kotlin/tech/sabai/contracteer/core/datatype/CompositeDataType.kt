package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.datatype.GenerationOutcome.Boundary
import tech.sabai.contracteer.core.datatype.GenerationOutcome.Reason
import tech.sabai.contracteer.core.datatype.GenerationOutcome.Value

/**
 * Base type for composite schemas (`allOf`, `anyOf`, `oneOf`) that combine multiple [subTypes].
 */
abstract class CompositeDataType<T>(
  name: String,
  openApiType: String,
  isNullable: Boolean,
  val subTypes: List<DataType<out T>>,
  dataTypeClass: Class<out T>,
  allowedValues: AllowedValues? = null): ResolvedDataType<T>(name, openApiType, isNullable, dataTypeClass, allowedValues) {

  abstract val discriminator: Discriminator?

  /**
   * Injects the discriminator property into [value] when both are present.
   *
   * [dataTypeName] identifies the schema whose mapping name to look up: the composite's
   * own name for `allOf` (the discriminated type itself), or the picked sub-type's name
   * for `oneOf`/`anyOf` (the sub-type identifies the variant).
   */
  @Suppress("UNCHECKED_CAST")
  protected fun injectDiscriminator(value: Any?, dataTypeName: String): Any? {
    val discriminator = this.discriminator
    return if (discriminator == null || value == null) value
    else (value as Map<String, Any?>) + (discriminator.propertyName to discriminator.getMappingName(dataTypeName))
  }

  /**
   * Walks [subTypes] in shuffled order and returns the first sub-type that produces a [Value],
   * with the discriminator injected. If every sub-type produces [Boundary], the first such
   * boundary is propagated; if [subTypes] is empty, falls back to [Reason.CYCLE].
   */
  protected fun generateFromAnySubType(ctx: GenerationContext): GenerationOutcome<Any> {
    var firstBoundary: Boundary? = null
    for (subType in subTypes.shuffled()) {
      when (val result = subType.randomValue(ctx).forProperty(subType.name)) {
        is Value    -> return Value(injectDiscriminator(result.value, subType.name))
        is Boundary -> firstBoundary = firstBoundary ?: result
      }
    }
    return firstBoundary ?: Boundary(Reason.CYCLE)
  }
}
