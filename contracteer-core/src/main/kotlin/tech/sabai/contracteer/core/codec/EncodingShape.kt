package tech.sabai.contracteer.core.codec

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.*

internal sealed class EncodingShape {
  object Scalar: EncodingShape()
  data class Object(val properties: Map<String, DecodeView>,
                    val additionalProperties: AdditionalPropertiesPolicy): EncodingShape()

  data class Array(val itemType: DecodeView): EncodingShape()
  data class Mixed(val reason: String): EncodingShape()

  data class AdditionalPropertiesPolicy(val allowed: Boolean, val itemType: DecodeView?)

  companion object {
    fun of(dataType: DataType<out Any>): Result<EncodingShape> =
      compute(dataType, CycleGuard())

    private fun compute(dataType: DataType<out Any>, guard: CycleGuard<Result<EncodingShape>>): Result<EncodingShape> =
      when (dataType) {
        is AnyDataType          -> success(Scalar)
        is ObjectDataType       -> ofObject(dataType)
        is ArrayDataType        -> success(Array(DecodeView.of(dataType.itemDataType)))
        is ProxyDataType        -> ofProxy(dataType, guard)
        is CompositeDataType<*> -> ofComposite(dataType, guard)
        else                    -> success(Scalar)
      }

    private fun ofObject(dataType: ObjectDataType): Result<EncodingShape> =
      success(Object(
        properties = dataType.properties.mapValues { (_, type) -> DecodeView.of(type) },
        additionalProperties = AdditionalPropertiesPolicy(
          allowed = dataType.allowAdditionalProperties,
          itemType = dataType.additionalPropertiesDataType?.let { DecodeView.of(it) }
        )
      ))

    private fun ofProxy(proxy: ProxyDataType, guard: CycleGuard<Result<EncodingShape>>): Result<EncodingShape> =
      if (!proxy.isResolved) success(Scalar)
      else guard.visit(proxy, onCycle = { success(cycleSentinel()) }) { compute(proxy.delegate, guard) }

    private fun cycleSentinel(): EncodingShape =
      Object(emptyMap(), AdditionalPropertiesPolicy(allowed = true, itemType = null))

    private fun ofComposite(composite: CompositeDataType<*>,
                            guard: CycleGuard<Result<EncodingShape>>): Result<EncodingShape> =
      composite.subTypes
        .filterNot { it is NullDataType }
        .map { compute(it, guard) }
        .combineResults()
        .flatMap { branches -> mergeBranches(composite, branches) }

    private fun mergeBranches(composite: CompositeDataType<*>,
                              branches: List<EncodingShape>): Result<EncodingShape> {
      val mixed = branches.filterIsInstance<Mixed>().firstOrNull()
      if (mixed != null) return success(mixed)
      if (branches.isEmpty()) return success(emptyCompositeShape(composite))

      val objects = branches.filterIsInstance<Object>()
      val arrays = branches.filterIsInstance<Array>()
      val scalars = branches.filterIsInstance<Scalar>()

      return when {
        objects.size == branches.size -> mergeObjects(composite, objects)
        arrays.size == branches.size  -> mergeArrays(arrays)
        scalars.size == branches.size -> success(Scalar)
        else                          -> success(Mixed("composite '${composite.name}' has branches of incompatible shapes"))
      }
    }

    private fun emptyCompositeShape(composite: CompositeDataType<*>): EncodingShape =
      Object(emptyMap(), AdditionalPropertiesPolicy(allowed = composite is AllOfDataType, itemType = null))

    private fun mergeObjects(composite: CompositeDataType<*>,
                             branches: List<Object>): Result<EncodingShape> {
      val flatEntries = branches.flatMap { branch -> branch.properties.map { it.key to it.value.source } }
      return mergeProperties(flatEntries).flatMap { props ->
        mergeAdditionalProperties(composite, branches.map { it.additionalProperties })
          .map { ap -> Object(props, ap) }
      }
    }

    private fun mergeProperties(entries: List<Pair<String, DataType<out Any>>>): Result<Map<String, DecodeView>> {
      val initial: Result<LinkedHashMap<String, DataType<out Any>>> = success(linkedMapOf())
      return entries
        .fold(initial) { acc, (name, incoming) -> acc.flatMap { it.combine(name, incoming) } }
        .map { sources -> sources.mapValues { (_, source) -> DecodeView.of(source) } }
    }

    private fun LinkedHashMap<String, DataType<out Any>>.combine(name: String,
                                                                 incoming: DataType<out Any>): Result<LinkedHashMap<String, DataType<out Any>>> {
      val existing = this[name]
      return when {
        existing == null                                   -> success(apply { put(name, incoming) })
        existing is AnyDataType || incoming is AnyDataType -> putAsAnyOf(name, existing, incoming)
        else                                               -> ensureCompatibleKinds(name, existing, incoming)
          .flatMap { putAsAnyOf(name, existing, incoming) }
      }
    }

    private fun LinkedHashMap<String, DataType<out Any>>.putAsAnyOf(name: String,
                                                                    existing: DataType<out Any>,
                                                                    incoming: DataType<out Any>): Result<LinkedHashMap<String, DataType<out Any>>> =
      AnyOfDataType.create(name, listOf(existing, incoming)).map { merged -> apply { put(name, merged) } }

    private fun ensureCompatibleKinds(name: String,
                                      existing: DataType<out Any>,
                                      incoming: DataType<out Any>): Result<Unit> =
      of(existing).flatMap { existingShape ->
        of(incoming).flatMap { incomingShape ->
          val existingKind = existingShape.kindLabel()
          val incomingKind = incomingShape.kindLabel()
          if (existingKind == incomingKind) success()
          else failure(name, "composed schemas define incompatible kinds: '$existingKind' vs '$incomingKind'")
        }
      }

    private fun EncodingShape.kindLabel(): String =
      when (this) {
        is Object -> "object"
        is Array  -> "array"
        else      -> "primitive"
      }

    private fun mergeArrays(branches: List<Array>): Result<EncodingShape> {
      val items = branches.map { it.itemType.source }
      return when {
        items.size == 1 -> success(Array(DecodeView.of(items.single())))
        else            -> AnyOfDataType.create("items", items).map { Array(DecodeView.of(it)) }
      }
    }

    private fun mergeAdditionalProperties(composite: CompositeDataType<*>,
                                          policies: List<AdditionalPropertiesPolicy>): Result<AdditionalPropertiesPolicy> =
      when (composite) {
        is AllOfDataType -> mergeAllOfAdditionalProperties(policies)
        else             -> mergeOrCompositionAdditionalProperties(policies)
      }

    private fun mergeAllOfAdditionalProperties(policies: List<AdditionalPropertiesPolicy>): Result<AdditionalPropertiesPolicy> {
      if (policies.any { !it.allowed }) return success(AdditionalPropertiesPolicy(false, null))

      val itemSources = policies.mapNotNull { it.itemType?.source }
      return when {
        itemSources.isEmpty() -> success(AdditionalPropertiesPolicy(true, null))
        itemSources.size == 1 -> success(AdditionalPropertiesPolicy(true, DecodeView.of(itemSources.single())))
        else                  -> success(AdditionalPropertiesPolicy(true,
                                                                    intersectAsAllOfOrNull(itemSources)?.let {
                                                                      DecodeView.of(it)
                                                                    }))
      }
    }

    private fun intersectAsAllOfOrNull(sources: List<DataType<out Any>>): DataType<out Any>? =
      (AllOfDataType.create("additionalProperties", sources) as? Result.Success)?.value

    private fun mergeOrCompositionAdditionalProperties(policies: List<AdditionalPropertiesPolicy>): Result<AdditionalPropertiesPolicy> {
      val allowingBranches = policies.filter { it.allowed }
      if (allowingBranches.isEmpty()) return success(AdditionalPropertiesPolicy(false, null))
      if (allowingBranches.any { it.itemType == null }) return success(AdditionalPropertiesPolicy(true, null))

      val itemSources = allowingBranches.map { it.itemType!!.source }
      return when {
        itemSources.size == 1 -> success(AdditionalPropertiesPolicy(true, DecodeView.of(itemSources.single())))
        else                  -> AnyOfDataType.create("additionalProperties", itemSources)
          .map { AdditionalPropertiesPolicy(true, DecodeView.of(it)) }
      }
    }
  }
}
