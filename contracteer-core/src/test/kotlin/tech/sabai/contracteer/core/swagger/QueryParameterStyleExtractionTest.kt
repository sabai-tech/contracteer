package tech.sabai.contracteer.core.swagger

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.datatype.AllOfDataType
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.ObjectDataType
import tech.sabai.contracteer.core.datatype.OneOfDataType
import tech.sabai.contracteer.core.operation.ParameterElement
import tech.sabai.contracteer.core.codec.DeepObjectParameterCodec
import tech.sabai.contracteer.core.codec.FormParameterCodec
import tech.sabai.contracteer.core.codec.PipeDelimitedParameterCodec
import tech.sabai.contracteer.core.codec.SpaceDelimitedParameterCodec
import kotlin.test.Test

class QueryParameterStyleExtractionTest {

  @Test
  fun `extracts query parameter with default style form and explode true`() {
    // when
    val operation = loadOperationByPath("/items")

    // then
    val queryParam = operation.requestSchema.queryParameters.single()
    assert(queryParam.element == ParameterElement.QueryParam("status"))
    assert(queryParam.dataType is ArrayDataType)
    assert(queryParam.codec is FormParameterCodec)
    assert((queryParam.codec as FormParameterCodec).explode)
  }

  @Test
  fun `extracts query parameter with style form explode false`() {
    // when
    val operation = loadOperationByPath("/form-no-explode")

    // then
    val queryParam = operation.requestSchema.queryParameters.single()
    assert(queryParam.codec is FormParameterCodec)
    assert(!(queryParam.codec as FormParameterCodec).explode)
  }

  @Test
  fun `extracts query parameter with style form explode true`() {
    // when
    val operation = loadOperationByPath("/form-explode")

    // then
    val queryParam = operation.requestSchema.queryParameters.single()
    assert(queryParam.codec is FormParameterCodec)
    assert((queryParam.codec as FormParameterCodec).explode)
  }

  @Test
  fun `extracts query parameter with style spaceDelimited`() {
    // when
    val operation = loadOperationByPath("/space-delimited")

    // then
    val queryParam = operation.requestSchema.queryParameters.single()
    assert(queryParam.codec is SpaceDelimitedParameterCodec)
  }

  @Test
  fun `extracts query parameter with style pipeDelimited`() {
    // when
    val operation = loadOperationByPath("/pipe-delimited")

    // then
    val queryParam = operation.requestSchema.queryParameters.single()
    assert(queryParam.codec is PipeDelimitedParameterCodec)
  }

  @Test
  fun `extracts query parameter object with style deepObject`() {
    // when
    val operation = loadOperationByPath("/deep-object")

    // then
    val queryParam = operation.requestSchema.queryParameters.single()
    assert(queryParam.dataType is ObjectDataType)
    assert(queryParam.codec is DeepObjectParameterCodec)
    assert(queryParam.codec.paramName == "filter")
  }

  @Test
  fun `rejects query parameter with deepObject and explode false`() {
    // when
    val result = loadResult("query_deepobject_explode_false.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("filter") && it.contains("explode") })
  }

  @Test
  fun `rejects query parameter array with deepObject`() {
    // when
    val result = loadResult("query_deepobject_array_type.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("status") && it.contains("object type") })
  }

  @Test
  fun `rejects deepObject with nested object property`() {
    // when
    val result = loadResult("query_deepobject_nested_object.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("filter") && it.contains("nested") })
  }

  @Test
  fun `rejects deepObject with array property`() {
    // when
    val result = loadResult("query_deepobject_array_property.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("filter") && it.contains("nested") })
  }

  @Test
  fun `rejects query parameter object with spaceDelimited`() {
    // when
    val result = loadResult("query_spacedelimited_object.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("filter") && it.contains("array type") })
  }

  @Test
  fun `rejects query parameter object with pipeDelimited`() {
    // when
    val result = loadResult("query_pipedelimited_object.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("filter") && it.contains("array type") })
  }

  @Test
  fun `rejects query parameter with spaceDelimited and explode true`() {
    // when
    val result = loadResult("query_spacedelimited_explode_true.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("status") && it.contains("explode=false") })
  }

  @Test
  fun `rejects query parameter with pipeDelimited and explode true`() {
    // when
    val result = loadResult("query_pipedelimited_explode_true.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("status") && it.contains("explode=false") })
  }

  @Test
  fun `rejects form style query parameter with nested object property`() {
    // when
    val result = loadResult("query_form_nested_object.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("filter") && it.contains("nested") })
  }

  @Test
  fun `rejects form style query parameter with array property`() {
    // when
    val result = loadResult("query_form_array_property.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("filter") && it.contains("nested") })
  }

  @Test
  fun `rejects form style query parameter with array of objects`() {
    // when
    val result = loadResult("query_form_array_of_objects.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("inputs") && it.contains("nested") })
  }

  @Test
  fun `rejects form style query parameter with array of arrays`() {
    // when
    val result = loadResult("query_form_array_of_arrays.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("matrix") && it.contains("nested") })
  }

  @Test
  fun `rejects form-explode object with additionalProperties schema`() {
    // when
    val result = loadResult("query_form_explode_additional_properties_schema.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("color") && it.contains("deepObject") })
  }

  @Test
  fun `rejects form-explode object with additionalProperties false`() {
    // when
    val result = loadResult("query_form_explode_additional_properties_false.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("color") && it.contains("deepObject") })
  }

  @Test
  fun `accepts form-explode object with additionalProperties true`() {
    // when
    val operations = loadResult("query_form_explode_additional_properties_true.yaml").assertSuccess()

    // then
    val queryParam = operations.single().requestSchema.queryParameters.single()
    assert(queryParam.codec is FormParameterCodec)
    assert((queryParam.codec as FormParameterCodec).explode)
    assert(queryParam.dataType is ObjectDataType)
    assert((queryParam.dataType as ObjectDataType).allowAdditionalProperties)
    assert((queryParam.dataType).additionalPropertiesDataType == null)
  }

  @Test
  fun `accepts deepObject composite of objects`() {
    // when
    val operations = loadResult("query_deepobject_allof_object.yaml").assertSuccess()

    // then
    val queryParam = operations.single().requestSchema.queryParameters.single()
    assert(queryParam.dataType is AllOfDataType)
    assert(queryParam.codec is DeepObjectParameterCodec)
  }

  @Test
  fun `accepts deepObject oneOf of objects`() {
    // when
    val operations = loadResult("query_deepobject_oneof_object.yaml").assertSuccess()

    // then
    val queryParam = operations.single().requestSchema.queryParameters.single()
    assert(queryParam.dataType is OneOfDataType)
    assert(queryParam.codec is DeepObjectParameterCodec)
  }

  @Test
  fun `rejects deepObject composite with nested object property`() {
    // when
    val result = loadResult("query_deepobject_composite_nested_object.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("filter") && it.contains("nested") })
  }

  @Test
  fun `accepts spaceDelimited composite of arrays`() {
    // when
    val operations = loadResult("query_spacedelimited_composite_array.yaml").assertSuccess()

    // then
    val queryParam = operations.single().requestSchema.queryParameters.single()
    assert(queryParam.dataType is OneOfDataType)
    assert(queryParam.codec is SpaceDelimitedParameterCodec)
  }

  @Test
  fun `accepts pipeDelimited composite of arrays`() {
    // when
    val operations = loadResult("query_pipedelimited_composite_array.yaml").assertSuccess()

    // then
    val queryParam = operations.single().requestSchema.queryParameters.single()
    assert(queryParam.dataType is OneOfDataType)
    assert(queryParam.codec is PipeDelimitedParameterCodec)
  }

  @Test
  fun `accepts form composite of objects`() {
    // when
    val operations = loadResult("query_form_composite_object.yaml").assertSuccess()

    // then
    val queryParam = operations.single().requestSchema.queryParameters.single()
    assert(queryParam.dataType is AllOfDataType)
    assert(queryParam.codec is FormParameterCodec)
  }

  @Test
  fun `rejects form composite with nested object property`() {
    // when
    val result = loadResult("query_form_composite_nested_object.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("filter") && it.contains("nested") })
  }

  @Test
  fun `rejects form composite array of objects`() {
    // when
    val result = loadResult("query_form_composite_array_of_objects.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("inputs") && it.contains("nested") })
  }

  @Test
  fun `rejects form-explode composite with additionalProperties false`() {
    // when
    val result = loadResult("query_form_explode_composite_additional_properties_false.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("filter") && it.contains("deepObject") })
  }

  @Test
  fun `accepts form-explode oneOf when one branch is permissive (additionalProperties true)`() {
    // when
    val operations = loadResult("query_form_explode_oneof_permissive_branch.yaml").assertSuccess()

    // then
    val queryParam = operations.single().requestSchema.queryParameters.single()
    assert(queryParam.codec is FormParameterCodec)
    assert((queryParam.codec as FormParameterCodec).explode)
    assert(queryParam.dataType is OneOfDataType)
  }

  @ParameterizedTest(name = "rejects query parameter with unsupported style {0}")
  @ValueSource(strings = ["simple", "label", "matrix"])
  fun `parameterized test for all unsupported query parameter styles`(style: String) {
    // when
    val result = OpenApiLoader.loadOperations("src/test/resources/operation/parameter_style/query_invalid_styles.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("status") && it.contains(style) })
  }

  // --- Helpers ---

  private fun loadOperationByPath(path: String) =
    loadOperations()
      .single { it.path == path }

  private fun loadOperations() =
    OpenApiLoader.loadOperations("src/test/resources/operation/parameter_style/query_parameters.yaml").assertSuccess()

  private fun loadResult(yamlFile: String) =
    OpenApiLoader.loadOperations("src/test/resources/operation/parameter_style/$yamlFile")

}
