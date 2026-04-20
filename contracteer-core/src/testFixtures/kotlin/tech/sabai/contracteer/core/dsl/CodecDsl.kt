package tech.sabai.contracteer.core.dsl

import tech.sabai.contracteer.core.codec.*
import tech.sabai.contracteer.core.serde.Serde

typealias CodecFactory = (String) -> ParameterCodec

fun simple(explode: Boolean = false): CodecFactory =
  { SimpleParameterCodec(it, explode) }

fun matrix(explode: Boolean = false): CodecFactory =
  { MatrixParameterCodec(it, explode) }

fun label(explode: Boolean = false): CodecFactory =
  { LabelParameterCodec(it, explode) }

fun form(explode: Boolean = true, allowReserved: Boolean = false): CodecFactory =
  { FormParameterCodec(it, explode, allowReserved) }

fun spaceDelimited(allowReserved: Boolean = false): CodecFactory =
  { SpaceDelimitedParameterCodec(it, allowReserved) }

fun pipeDelimited(allowReserved: Boolean = false): CodecFactory =
  { PipeDelimitedParameterCodec(it, allowReserved) }

fun deepObject(allowReserved: Boolean = false): CodecFactory =
  { DeepObjectParameterCodec(it, allowReserved) }

fun content(serde: Serde, allowReserved: Boolean = false): CodecFactory =
  { ContentCodec(it, serde, allowReserved) }
