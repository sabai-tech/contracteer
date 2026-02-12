package tech.sabai.contracteer.core.operation

import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.serde.Serde

data class ApiOperation(
  val path: String,
  val method: String,
  val requestSchema: RequestSchema,
  val responses: Map<Int, ResponseSchema>,
  val scenarios: List<Scenario>
)

data class RequestSchema(
  val parameters: List<ParameterSchema>,
  val bodies: List<BodySchema>
) {
  val pathParameters get() = parameters.filter { it.element is ParameterElement.PathParam }
  val queryParameters get() = parameters.filter { it.element is ParameterElement.QueryParam }
  val headers get() = parameters.filter { it.element is ParameterElement.Header }
  val cookies get() = parameters.filter { it.element is ParameterElement.Cookie }
}

data class ResponseSchema(
  val headers: List<ParameterSchema>,
  val bodies: List<BodySchema>
)

data class ParameterSchema(
  val element: ParameterElement,
  val dataType: DataType<out Any>,
  val isRequired: Boolean,
  val serde: Serde
)

data class BodySchema(
  val contentType: ContentType,
  val dataType: DataType<out Any>,
  val isRequired: Boolean
)
