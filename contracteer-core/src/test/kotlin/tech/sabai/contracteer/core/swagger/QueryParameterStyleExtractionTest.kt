package tech.sabai.contracteer.core.swagger

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.ObjectDataType
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
