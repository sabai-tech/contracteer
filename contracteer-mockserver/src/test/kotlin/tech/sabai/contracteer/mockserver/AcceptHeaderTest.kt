package tech.sabai.contracteer.mockserver

import tech.sabai.contracteer.core.operation.ContentType
import kotlin.test.Test

class AcceptHeaderTest {

  @Test
  fun `parses single media type`() {
    val accept = AcceptHeader.parse("application/json")
    assert(accept.mediaTypes.size == 1)
    assert(accept.mediaTypes.first().type == "application/json")
    assert(accept.mediaTypes.first().quality == 1.0)
  }

  @Test
  fun `parses multiple media types`() {
    val accept = AcceptHeader.parse("application/json, text/plain")
    assert(accept.mediaTypes.size == 2)
    assert(accept.mediaTypes[0].type == "application/json")
    assert(accept.mediaTypes[1].type == "text/plain")
  }

  @Test
  fun `parses quality factors`() {
    val accept = AcceptHeader.parse("text/plain;q=0.5, application/json;q=0.9")
    assert(accept.mediaTypes.size == 2)
    assert(accept.mediaTypes[0].type == "text/plain")
    assert(accept.mediaTypes[0].quality == 0.5)
    assert(accept.mediaTypes[1].type == "application/json")
    assert(accept.mediaTypes[1].quality == 0.9)
  }

  @Test
  fun `defaults quality to 1 when not specified`() {
    val accept = AcceptHeader.parse("application/json, text/plain;q=0.5")
    assert(accept.mediaTypes[0].quality == 1.0)
    assert(accept.mediaTypes[1].quality == 0.5)
  }

  @Test
  fun `accepts any returns true for null header`() {
    assert(AcceptHeader.parse(null).acceptsAny())
  }

  @Test
  fun `accepts any returns true for empty header`() {
    assert(AcceptHeader.parse("").acceptsAny())
  }

  @Test
  fun `accepts any returns true for wildcard`() {
    assert(AcceptHeader.parse("*/*").acceptsAny())
  }

  @Test
  fun `accepts any returns false for specific type`() {
    assert(!AcceptHeader.parse("application/json").acceptsAny())
  }

  @Test
  fun `best match selects highest quality matching type`() {
    // given
    val accept = AcceptHeader.parse("text/plain;q=0.5, application/json;q=0.9")
    val candidates = listOf(ContentType("text/plain"), ContentType("application/json"))

    // when
    val best = accept.bestMatch(candidates)

    // then
    assert(best?.value == "application/json")
  }

  @Test
  fun `best match returns null when no match`() {
    // given
    val accept = AcceptHeader.parse("image/png")
    val candidates = listOf(ContentType("application/json"), ContentType("text/plain"))

    // when
    val best = accept.bestMatch(candidates)

    // then
    assert(best == null)
  }

  @Test
  fun `best match with wildcard matches first candidate`() {
    // given
    val accept = AcceptHeader.parse("*/*")
    val candidates = listOf(ContentType("application/json"), ContentType("text/plain"))

    // when
    val best = accept.bestMatch(candidates)

    // then
    assert(best?.value == "application/json")
  }

  @Test
  fun `best match with subtype wildcard`() {
    // given
    val accept = AcceptHeader.parse("application/*")
    val candidates = listOf(ContentType("text/plain"), ContentType("application/json"))

    // when
    val best = accept.bestMatch(candidates)

    // then
    assert(best?.value == "application/json")
  }

  @Test
  fun `accepts any returns false for wildcard with quality zero`() {
    assert(!AcceptHeader.parse("*/*;q=0").acceptsAny())
  }

  @Test
  fun `best match excludes types with quality zero`() {
    // given
    val accept = AcceptHeader.parse("application/json;q=0, text/plain")
    val candidates = listOf(ContentType("application/json"), ContentType("text/plain"))

    // when
    val best = accept.bestMatch(candidates)

    // then
    assert(best?.value == "text/plain")
  }

  @Test
  fun `best match prefers exact over wildcard at same quality`() {
    // given
    val accept = AcceptHeader.parse("application/json, */*;q=0.1")
    val candidates = listOf(ContentType("text/plain"), ContentType("application/json"))

    // when
    val best = accept.bestMatch(candidates)

    // then
    assert(best?.value == "application/json")
  }
}