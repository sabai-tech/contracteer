package tech.sabai.contracteer.verifier.junit

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer

object TestServer {

  fun start(port: Int) =
    routes(
      "/products/{id}" bind GET to { req ->
        if (req.path("id")!! == "999") {
          Response(NOT_FOUND)
            .header("Content-type", "application/json")
            .body("""{ "error": "NOT FOUND"}""")
        } else {
          Response(OK)
            .header("Content-type", "application/json")
            .body("""{ "id": 123, "name": "John Doe", "quantity": 42}""")
        }
      }
    ).asServer(SunHttp(port)).start()
}