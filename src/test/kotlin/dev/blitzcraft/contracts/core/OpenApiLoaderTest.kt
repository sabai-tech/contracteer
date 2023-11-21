package dev.blitzcraft.contracts.core

import java.nio.file.Path
import kotlin.test.Test

class OpenApiLoaderTest {

  @Test
  fun `does not load Open API when 2xx response is missing`() {
    // when
    val result = OpenApiLoader.from(Path.of("src/test/resources/api_missing_2xx_response.yaml"))

    // then
    assert(result.openAPI == null)
    assert(result.errors.size == 2)
  }
}



