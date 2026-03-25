package tech.sabai.contracteer.core.swagger

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.ObjectDataType
import tech.sabai.contracteer.core.operation.ParameterElement
import tech.sabai.contracteer.core.codec.LabelStyleCodec
import tech.sabai.contracteer.core.codec.MatrixStyleCodec
import tech.sabai.contracteer.core.codec.SimpleStyleCodec
import kotlin.test.Test

class PathParameterStyleExtractionTest {

  @Test
  fun `extracts path parameter with default style simple and explode false`() {
    // when
    val operation = loadOperationByPath("/items/{id}")

    // then
    val pathParam = operation.requestSchema.pathParameters.single()
    assert(pathParam.element == ParameterElement.PathParam("id"))
    assert(pathParam.dataType is ArrayDataType)
    assert(pathParam.codec is SimpleStyleCodec)
    assert((pathParam.codec as SimpleStyleCodec).explode == false)
  }

  @Test
  fun `extracts path parameter with style simple explode false`() {
    // when
    val operation = loadOperationByPath("/simple-no-explode/{id}")

    // then
    val pathParam = operation.requestSchema.pathParameters.single()
    assert(pathParam.codec is SimpleStyleCodec)
    assert((pathParam.codec as SimpleStyleCodec).explode == false)
  }

  @Test
  fun `extracts path parameter with style simple explode true`() {
    // when
    val operation = loadOperationByPath("/simple-explode/{id}")

    // then
    val pathParam = operation.requestSchema.pathParameters.single()
    assert(pathParam.codec is SimpleStyleCodec)
    assert((pathParam.codec as SimpleStyleCodec).explode == true)
  }

  @Test
  fun `extracts path parameter with style label explode false`() {
    // when
    val operation = loadOperationByPath("/label-no-explode/{id}")

    // then
    val pathParam = operation.requestSchema.pathParameters.single()
    assert(pathParam.codec is LabelStyleCodec)
    assert((pathParam.codec as LabelStyleCodec).explode == false)
  }

  @Test
  fun `extracts path parameter with style label explode true`() {
    // when
    val operation = loadOperationByPath("/label-explode/{id}")

    // then
    val pathParam = operation.requestSchema.pathParameters.single()
    assert(pathParam.codec is LabelStyleCodec)
    assert((pathParam.codec as LabelStyleCodec).explode == true)
  }

  @Test
  fun `extracts path parameter with style matrix explode false`() {
    // when
    val operation = loadOperationByPath("/matrix-no-explode/{id}")

    // then
    val pathParam = operation.requestSchema.pathParameters.single()
    assert(pathParam.codec is MatrixStyleCodec)
    assert((pathParam.codec as MatrixStyleCodec).explode == false)
    assert(pathParam.codec.paramName == "id")
  }

  @Test
  fun `extracts path parameter with style matrix explode true`() {
    // when
    val operation = loadOperationByPath("/matrix-explode/{id}")

    // then
    val pathParam = operation.requestSchema.pathParameters.single()
    assert(pathParam.codec is MatrixStyleCodec)
    assert((pathParam.codec as MatrixStyleCodec).explode == true)
    assert(pathParam.codec.paramName == "id")
  }

  @Test
  fun `extracts path parameter object with style simple`() {
    // when
    val operation = loadOperationByPath("/object-simple/{filter}")

    // then
    val pathParam = operation.requestSchema.pathParameters.single()
    assert(pathParam.dataType is ObjectDataType)
    assert(pathParam.codec is SimpleStyleCodec)
  }

  @ParameterizedTest(name = "rejects path parameter with unsupported style {0}")
  @ValueSource(strings = ["form", "spaceDelimited", "pipeDelimited", "deepObject"])
  fun `parameterized test for all unsupported path parameter styles`(style: String) {
    // when
    val result = loadInvalidStyleOperationsResult()

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("id") && it.contains(style) })
  }

  // --- Helpers ---

  private fun loadOperationByPath(path: String) =
    OpenApiLoader.loadOperations("src/test/resources/operation/parameter_style/path_parameters.yaml").assertSuccess()
      .single { it.path == path }

  private fun loadInvalidStyleOperationsResult() =
    OpenApiLoader.loadOperations("src/test/resources/operation/parameter_style/path_invalid_styles.yaml")
}
