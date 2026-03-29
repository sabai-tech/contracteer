package tech.sabai.contracteer.core.operation

import org.junit.jupiter.api.Test

class ContentTypeTest {

  @Test
  fun `validates when content-types are equal`() {
    assert(ContentType("application/json").validate("application/json").isSuccess())
  }

  @Test
  fun `does not validate when content-types are not equal`() {
    assert(ContentType("application/json").validate("application/xml").isFailure())
  }

  @Test
  fun `wildcard matches any content type`() {
    assert(ContentType("*/*").validate("application/json").isSuccess())
  }

  @Test
  fun `subtype wildcard matches same type`() {
    assert(ContentType("image/*").validate("image/jpeg").isSuccess())
  }

  @Test
  fun `subtype wildcard does not match different type`() {
    assert(ContentType("image/*").validate("text/plain").isFailure())
  }

  @Test
  fun `matches when actual has parameters`() {
    assert(ContentType("multipart/form-data").validate("multipart/form-data; boundary=abc").isSuccess())
  }

  @Test
  fun `matches ignoring case`() {
    assert(ContentType("application/json").validate("Application/JSON").isSuccess())
  }
}