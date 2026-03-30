package tech.sabai.contracteer.core.swagger

import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.operation.ParameterElement.QueryParam
import tech.sabai.contracteer.core.serde.FormUrlEncodedSerde
import kotlin.test.Test

class AllowReservedExtractionTest {

  @Test
  fun `extracts query parameter with allowReserved true`() {
    // when
    val operation = loadQueryOperations().single { it.path == "/search" }

    // then
    val queryParam = operation.requestSchema.queryParameters.single()
    assert(queryParam.element == QueryParam("path", allowReserved = true))
    assert((queryParam.element as QueryParam).allowReserved)
  }

  @Test
  fun `extracts query parameter with allowReserved false`() {
    // when
    val operation = loadQueryOperations().single { it.path == "/filter" }

    // then
    val queryParam = operation.requestSchema.queryParameters.single()
    assert(queryParam.element == QueryParam("filter", allowReserved = false))
    assert(!(queryParam.element as QueryParam).allowReserved)
  }

  @Test
  fun `extracts query parameter with default allowReserved as false`() {
    // when
    val operation = loadQueryOperations().single { it.path == "/default" }

    // then
    val queryParam = operation.requestSchema.queryParameters.single()
    assert(queryParam.element == QueryParam("q"))
    assert(!(queryParam.element as QueryParam).allowReserved)
  }

  @Test
  fun `extracts form-urlencoded with allowReserved on encoding property`() {
    // when
    val operation = loadFormOperations().single { it.path == "/submit" }

    // then
    val serde = operation.requestSchema.bodies.single().serde as FormUrlEncodedSerde
    assert(serde.propertyEncodings.getValue("callback").allowReserved)
    assert(!serde.propertyEncodings.getValue("name").allowReserved)
  }

  // --- Helpers ---

  private fun loadQueryOperations() =
    OpenApiLoader.loadOperations("src/test/resources/operation/allow_reserved/query_allow_reserved.yaml").assertSuccess()

  private fun loadFormOperations() =
    OpenApiLoader.loadOperations("src/test/resources/operation/allow_reserved/form_urlencoded_allow_reserved.yaml").assertSuccess()
}