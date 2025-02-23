package tech.sabai.contracteer.core.contract

import org.junit.jupiter.api.Test

class ContentTypeTest {


  @Test
  fun `validates when content-types are equal`() {
    // given
    val contentType = ContentType("application-json")

    // when
    val result = contentType.validate("application-json")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate when content-types are not equal`() {
    // given
    val contentType = ContentType("application-json")

    // when
    val result = contentType.validate("application-xml")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `Content type with only wildcard validates all`() {
    // given
    val contentType = ContentType("*/*")

    // when
    val result = contentType.validate("application-json")

    // then
    assert(result.isSuccess())
  }
  @Test
  fun `Content type containing a wildcard validates when the root are the same`() {
    // given
    val contentType = ContentType("image/*")

    // when
    val result = contentType.validate("image/jpeg")

    // then
    assert(result.isSuccess())
  }
}