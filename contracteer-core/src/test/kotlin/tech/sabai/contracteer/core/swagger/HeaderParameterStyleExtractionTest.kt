package tech.sabai.contracteer.core.swagger

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.ObjectDataType
import tech.sabai.contracteer.core.operation.ParameterElement
import tech.sabai.contracteer.core.codec.SimpleParameterCodec
import kotlin.test.Test

class HeaderParameterStyleExtractionTest {

  @Test
  fun `extracts header parameter with default style simple and explode false`() {
    // when
    val operation = loadOperationByPath("/items")

    // then
    val headerParam = operation.requestSchema.headers.single()
    assert(headerParam.element == ParameterElement.Header("X-Status"))
    assert(headerParam.dataType is ArrayDataType)
    assert(headerParam.codec is SimpleParameterCodec)
    assert((headerParam.codec as SimpleParameterCodec).explode == false)
  }

  @Test
  fun `extracts header parameter with style simple explode false`() {
    // when
    val operation = loadOperationByPath("/simple-no-explode")

    // then
    val headerParam = operation.requestSchema.headers.single()
    assert(headerParam.codec is SimpleParameterCodec)
    assert((headerParam.codec as SimpleParameterCodec).explode == false)
  }

  @Test
  fun `extracts header parameter with style simple explode true`() {
    // when
    val operation = loadOperationByPath("/simple-explode")

    // then
    val headerParam = operation.requestSchema.headers.single()
    assert(headerParam.codec is SimpleParameterCodec)
    assert((headerParam.codec as SimpleParameterCodec).explode == true)
  }

  @Test
  fun `extracts header parameter object with style simple`() {
    // when
    val operation = loadOperationByPath("/object-simple")

    // then
    val headerParam = operation.requestSchema.headers.single()
    assert(headerParam.dataType is ObjectDataType)
    assert(headerParam.codec is SimpleParameterCodec)
  }

  @Test
  fun `extracts response header with default style simple`() {
    // when
    val operation = loadOperationByPath("/response-header-default")

    // then
    val responseSchema = operation.responseFor(200)!!
    val header = responseSchema.headers.single()
    assert(header.element == ParameterElement.Header("X-Rate-Limit"))
    assert(header.dataType is ArrayDataType)
    assert(header.codec is SimpleParameterCodec)
    assert((header.codec as SimpleParameterCodec).explode == false)
  }

  @Test
  fun `extracts response header object with style simple`() {
    // when
    val operation = loadOperationByPath("/response-header-object")

    // then
    val responseSchema = operation.responseFor(200)!!
    val header = responseSchema.headers.single()
    assert(header.dataType is ObjectDataType)
    assert(header.codec is SimpleParameterCodec)
  }

  @ParameterizedTest(name = "rejects header parameter with unsupported style {0}")
  @ValueSource(strings = ["form", "label", "matrix", "spaceDelimited", "pipeDelimited", "deepObject"])
  fun `parameterized test for all unsupported header parameter styles`(style: String) {
    // when
    val result = loadInvalidStyleOperationsResult()

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("X-Status") && it.contains(style) })
  }

  // --- Helpers ---

  private fun loadOperationByPath(path: String) =
    loadOperations()
      .single { it.path == path }

  private fun loadOperations() =
    OpenApiLoader.loadOperations("src/test/resources/operation/parameter_style/header_parameters.yaml").assertSuccess()

  private fun loadInvalidStyleOperationsResult() =
    OpenApiLoader.loadOperations("src/test/resources/operation/parameter_style/header_invalid_styles.yaml")
}
