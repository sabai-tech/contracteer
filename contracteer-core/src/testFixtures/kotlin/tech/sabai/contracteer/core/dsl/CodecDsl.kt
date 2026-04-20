package tech.sabai.contracteer.core.dsl

import tech.sabai.contracteer.core.codec.ContentCodec
import tech.sabai.contracteer.core.codec.DeepObjectParameterCodec
import tech.sabai.contracteer.core.codec.FormParameterCodec
import tech.sabai.contracteer.core.codec.LabelParameterCodec
import tech.sabai.contracteer.core.codec.MatrixParameterCodec
import tech.sabai.contracteer.core.codec.ParameterCodec
import tech.sabai.contracteer.core.codec.PipeDelimitedParameterCodec
import tech.sabai.contracteer.core.codec.SimpleParameterCodec
import tech.sabai.contracteer.core.codec.SpaceDelimitedParameterCodec
import tech.sabai.contracteer.core.serde.Serde

typealias CodecFactory = (String) -> ParameterCodec

fun simple(explode: Boolean = false): CodecFactory = { SimpleParameterCodec(it, explode) }
fun matrix(explode: Boolean = false): CodecFactory = { MatrixParameterCodec(it, explode) }
fun label(explode: Boolean = false): CodecFactory = { LabelParameterCodec(it, explode) }
fun form(explode: Boolean = true): CodecFactory = { FormParameterCodec(it, explode) }
fun spaceDelimited(): CodecFactory = ::SpaceDelimitedParameterCodec
fun pipeDelimited(): CodecFactory = ::PipeDelimitedParameterCodec
fun deepObject(): CodecFactory = ::DeepObjectParameterCodec
fun content(serde: Serde): CodecFactory = { ContentCodec(it, serde) }
