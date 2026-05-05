package tech.sabai.contracteer.core.swagger

import tech.sabai.contracteer.core.assertFailure
import kotlin.test.Test

class OpenApiLoaderVersionTest {

  @Test
  fun `rejects OpenAPI 3 1 with a not-yet-supported message`() {
    // when
    val result = OpenApiLoader.loadOperations("classpath:error/openapi_31_unsupported.yaml")

    // then
    val errors = result.assertFailure()
    assert(errors.any { it == "OpenAPI 3.1 is not yet supported. Contracteer currently supports OpenAPI 3.0.x." })
  }

  @Test
  fun `rejects OpenAPI 3 2 as unsupported`() {
    // when
    val result = OpenApiLoader.loadOperations("classpath:error/openapi_32_unsupported.yaml")

    // then
    val errors = result.assertFailure()
    assert(errors.any { it == "OpenAPI version '3.2.0' is not supported. Contracteer currently supports OpenAPI 3.0.x." })
  }

  @Test
  fun `rejects OpenAPI 4 x as unsupported`() {
    // when
    val result = OpenApiLoader.loadOperations("classpath:error/openapi_4x_unsupported.yaml")

    // then
    val errors = result.assertFailure()
    assert(errors.any { it == "OpenAPI version '4.0.0' is not supported. Contracteer currently supports OpenAPI 3.0.x." })
  }

  @Test
  fun `rejects OpenAPI 2 x as unsupported`() {
    // when
    val result = OpenApiLoader.loadOperations("classpath:error/openapi_2x_unsupported.yaml")

    // then
    val errors = result.assertFailure()
    assert(errors.any { it == "OpenAPI version '2.0' is not supported. Contracteer currently supports OpenAPI 3.0.x." })
  }

  @Test
  fun `rejects document with missing openapi version field`() {
    // when
    val result = OpenApiLoader.loadOperations("classpath:error/openapi_missing_version.yaml")

    // then
    val errors = result.assertFailure()
    assert(errors.any { it == "OpenAPI document does not declare a version. Contracteer currently supports OpenAPI 3.0.x." })
  }
}
