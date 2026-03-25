package tech.sabai.contracteer.mockserver

import org.http4k.core.Request
import org.http4k.core.cookie.cookie
import org.http4k.routing.path
import tech.sabai.contracteer.core.operation.ParameterElement
import tech.sabai.contracteer.core.operation.ParameterElement.*

internal fun Request.valueExtractorFor(element: ParameterElement): (String) -> List<String> =
  when (element) {
    is PathParam  -> { key -> listOfNotNull(path(key)) }
    is QueryParam -> { key -> queries(key).filterNotNull() }
    is Header     -> { key -> headerValues(key).filterNotNull() }
    is Cookie     -> { key -> listOfNotNull(cookie(key)?.value) }
  }
