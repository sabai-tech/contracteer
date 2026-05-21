package tech.sabai.contracteer.mockserver

import org.http4k.core.Parameters
import org.http4k.core.Request
import org.http4k.core.cookie.cookie
import org.http4k.core.queries
import org.http4k.routing.path
import tech.sabai.contracteer.core.operation.ParameterElement
import tech.sabai.contracteer.core.operation.ParameterElement.*

internal fun Request.valuesFor(element: ParameterElement): Map<String, List<String>> =
  when (element) {
    is PathParam  -> path(element.name)?.let { mapOf(element.name to listOf(it)) } ?: emptyMap()
    is QueryParam -> uri.queries().groupNonNullValues()
    is Header     -> headerValues(element.name).filterNotNull()
                       .takeIf { it.isNotEmpty() }
                       ?.let { mapOf(element.name to it) } ?: emptyMap()
    is Cookie     -> cookie(element.name)?.let { mapOf(element.name to listOf(it.value)) } ?: emptyMap()
  }

private fun Parameters.groupNonNullValues(): Map<String, List<String>> =
  filter { it.second != null }.groupBy({ it.first }, { it.second!! })
