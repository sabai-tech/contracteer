package tech.sabai.contracteer.core.swagger

import tech.sabai.contracteer.core.assertFailure
import tech.sabai.contracteer.core.assertSuccess
import kotlin.test.Test

class ClasspathLoadingTest {

  @Test
  fun `succeeds from an existing classpath resource`() {
    // when
    val operations = OpenApiLoader.loadOperations("classpath:scenario/2xx_schema_with_4xx_scenario.yaml").assertSuccess()

    // then
    assert(operations.size == 1)
  }

  @Test
  fun `succeeds from an existing classpath resource with leading slash`() {
    // when
    val operations = OpenApiLoader.loadOperations("classpath:/scenario/2xx_schema_with_4xx_scenario.yaml").assertSuccess()

    // then
    assert(operations.size == 1)
  }

  @Test
  fun `fails when classpath resource does not exist`() {
    // when
    val result = OpenApiLoader.loadOperations("classpath:not_found.yaml")

    // then
    val errors = result.assertFailure()
    assert(errors.single() == "Classpath resource not found: not_found.yaml")
  }
}
