package tech.sabai.contracteer.mockserver

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.UriTemplate
import org.http4k.routing.Router
import org.http4k.routing.RouterDescription
import org.http4k.routing.RouterMatch
import org.http4k.routing.RouterMatch.MatchingHandler
import org.http4k.routing.RouterMatch.MethodNotMatched
import org.http4k.routing.RouterMatch.Unmatched
import org.http4k.routing.RoutedRequest
import java.util.regex.Pattern

/**
 * A [Router] that matches requests by method and an exact path template, preserving leading and
 * trailing slashes. Unlike http4k's built-in [org.http4k.core.UriTemplate], which trims slashes
 * on both the template and the request path, this router treats `/foo` and `/foo/` as distinct.
 *
 * OpenAPI allows two operations to differ only by a trailing slash; the mock server must be
 * able to dispatch each one to its own handler. The underlying [UriTemplate] is still used for
 * path-parameter extraction once a match is established.
 */
internal class StrictPathRouter(
  path: String,
  private val method: Method,
  private val handler: HttpHandler): Router {

  private val pathRegex = compilePathRegex(path)
  private val template = UriTemplate.from(path)

  override val description = RouterDescription("strict-path == '$path' method == '$method'")

  override fun match(request: Request): RouterMatch =
    when {
      !pathRegex.matcher(request.uri.path).matches() -> Unmatched(description)
      method != request.method                       -> MethodNotMatched(description)
      else                                           ->
        MatchingHandler({ handler(RoutedRequest(it, template)) }, description)
    }

  companion object {
    private val PARAM_SEGMENT = Regex("""\{([^}]+)}""")

    private fun compilePathRegex(path: String): Pattern =
      Pattern.compile(path.split(PARAM_SEGMENT).joinToString(separator = "([^/]+)") { Pattern.quote(it) })
  }
}
