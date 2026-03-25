package tech.sabai.contracteer.core.swagger

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.ObjectDataType
import tech.sabai.contracteer.core.operation.ParameterElement
import tech.sabai.contracteer.core.codec.FormStyleCodec
import kotlin.test.Test

class CookieParameterStyleExtractionTest {

  @Test
  fun `extracts cookie parameter with default style form and explode true`() {
    // when
    val operation = loadOperationByPath("/items")

    // then
    val cookieParam = operation.requestSchema.cookies.single()
    assert(cookieParam.element == ParameterElement.Cookie("session"))
    assert(cookieParam.dataType is ArrayDataType)
    assert(cookieParam.codec is FormStyleCodec)
    assert((cookieParam.codec as FormStyleCodec).explode == true)
  }

  @Test
  fun `extracts cookie parameter with style form explode false`() {
    // when
    val operation = loadOperationByPath("/form-no-explode")

    // then
    val cookieParam = operation.requestSchema.cookies.single()
    assert(cookieParam.codec is FormStyleCodec)
    assert((cookieParam.codec as FormStyleCodec).explode == false)
  }

  @Test
  fun `extracts cookie parameter with style form explode true`() {
    // when
    val operation = loadOperationByPath("/form-explode")

    // then
    val cookieParam = operation.requestSchema.cookies.single()
    assert(cookieParam.codec is FormStyleCodec)
    assert((cookieParam.codec as FormStyleCodec).explode == true)
  }

  @Test
  fun `extracts cookie parameter object with style form`() {
    // when
    val operation = loadOperationByPath("/object-form")

    // then
    val cookieParam = operation.requestSchema.cookies.single()
    assert(cookieParam.dataType is ObjectDataType)
    assert(cookieParam.codec is FormStyleCodec)
  }

  @ParameterizedTest(name = "rejects cookie parameter with unsupported style {0}")
  @ValueSource(strings = ["simple", "label", "matrix", "spaceDelimited", "pipeDelimited", "deepObject"])
  fun `parameterized test for all unsupported cookie parameter styles`(style: String) {
    // when
    val result = loadInvalidStyleOperationsResult()

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("session") && it.contains(style) })
  }

  // --- Helpers ---

  private fun loadOperationByPath(path: String) =
    loadOperations()
      .single { it.path == path }

  private fun loadOperations() =
    OpenApiLoader.loadOperations("src/test/resources/operation/parameter_style/cookie_parameters.yaml").assertSuccess()

  private fun loadInvalidStyleOperationsResult() =
    OpenApiLoader.loadOperations("src/test/resources/operation/parameter_style/cookie_invalid_styles.yaml")
}
