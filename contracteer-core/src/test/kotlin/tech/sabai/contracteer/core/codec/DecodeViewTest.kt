package tech.sabai.contracteer.core.codec

import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.dsl.objectType
import tech.sabai.contracteer.core.dsl.stringType
import tech.sabai.contracteer.core.serde.PlainTextSerde
import kotlin.test.Test

class DecodeViewTest {

  @Test
  fun `PlainTextSerde deserialize through DecodeView delegates to wrapped source`() {
    // given
    val view = DecodeView.of(stringType())

    // when
    val result = PlainTextSerde.deserialize("hello", view).assertSuccess()

    // then
    assert(result == "hello")
  }

  @Test
  fun `shape returns the wrapped source's EncodingShape`() {
    // given
    val source = objectType { properties { "name" to stringType() } }
    val view = DecodeView.of(source)

    // when
    val shape = view.shape().assertSuccess()

    // then
    assert(shape is EncodingShape.Object)
  }

  @Test
  fun `equals is based on source identity`() {
    // given
    val source = stringType()

    // when
    val viewA = DecodeView.of(source)
    val viewB = DecodeView.of(source)
    val viewC = DecodeView.of(stringType())

    // then
    assert(viewA == viewB)
    assert(viewA != viewC)
  }
}
