package tech.sabai.contracteer.core.swagger

import tech.sabai.contracteer.core.assertFailure
import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.codec.ContentCodec
import tech.sabai.contracteer.core.datatype.ObjectDataType
import tech.sabai.contracteer.core.operation.ParameterElement
import tech.sabai.contracteer.core.serde.JsonSerde
import kotlin.test.Test

class ParameterContentExtractionTest {

  @Test
  fun `extracts query parameter with content application json`() {
    // when
    val operation = loadOperations("query_content_parameter.yaml").single { it.path == "/items" }

    // then
    val queryParam = operation.requestSchema.queryParameters.single()
    assert(queryParam.element == ParameterElement.QueryParam("filter"))
    assert(queryParam.dataType is ObjectDataType)
    assert(queryParam.codec is ContentCodec)
    assert((queryParam.codec as ContentCodec).serde is JsonSerde)
  }

  @Test
  fun `extracts header parameter with content application json`() {
    // when
    val operation = loadOperations("header_content_parameter.yaml").single { it.path == "/items" }

    // then
    val headerParam = operation.requestSchema.headers.single()
    assert(headerParam.element == ParameterElement.Header("X-Filter"))
    assert(headerParam.dataType is ObjectDataType)
    assert(headerParam.codec is ContentCodec)
  }

  @Test
  fun `extraction fails when content has multiple entries`() {
    // when
    val result = OpenApiLoader.loadOperations(
      "src/test/resources/operation/parameter_content/multiple_content_entries.yaml")

    // then
    val errors = result.assertFailure()
    assert(errors.any { it.contains("multiple media types") })
  }

  @Test
  fun `extracts scenarios from content-level examples`() {
    // when
    val operation = loadOperations("content_with_examples.yaml").single { it.path == "/items" }

    // then
    assert(operation.scenarios.size == 1)
    val scenario = operation.scenarios.single()
    assert(scenario.key == "active_filter")
    assert(scenario.request.parameterValues.any { (element, _) -> element is ParameterElement.QueryParam })
  }

  private fun loadOperations(file: String) =
    OpenApiLoader.loadOperations("src/test/resources/operation/parameter_content/$file").assertSuccess()
}
