package tech.sabai.contracteer.core.swagger

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.operation.ApiOperation
import java.io.File
import java.net.*

/**
 * Loads and parses an OpenAPI 3.0 specification into a list of [ApiOperation] instances.
 *
 * Accepts a file path or an HTTP(S) URL pointing to an OpenAPI document.
 */
object OpenApiLoader {

  /**
   * Parses the OpenAPI document at the given [path] and extracts all API operations.
   *
   * @param path a local file path or an HTTP(S) URL to an OpenAPI 3.0 document
   * @return a [Result] containing the list of extracted [ApiOperation] instances,
   *         or errors if the document is invalid or cannot be loaded
   */
  @JvmStatic
  fun loadOperations(path: String): Result<List<ApiOperation>> =
    path.loadOpenApiDocument()
      .flatMap { parse(it!!) }
      .flatMap { checkFor2xxResponses(it!!) }
      .flatMap {
        val openAPI = it!!
        val sharedComponents = SharedComponents(
          schemas = openAPI.components.safeSchemas(),
          parameters = openAPI.components.safeParameters(),
          requestBodies = openAPI.components.safeRequestBodies(),
          headers = openAPI.components.safeHeaders(),
          examples = openAPI.components.safeExamples(),
          responses = openAPI.components.safeResponses()
        )
        ApiOperationExtractor(sharedComponents).extract(openAPI)
      }

  private fun String.loadOpenApiDocument() =
    if (isUrl()) loadFromUrl(this) else loadFromFile(this)

  private fun loadFromFile(path: String): Result<String> {
    val file = File(path)
    return if (file.exists())
      success(file.readText())
    else
      failure("Invalid file: file not found at ${file.absoluteFile}")
  }

  private fun loadFromUrl(path: String) =
    path.toUrl().flatMap { it!!.loadOpenApiDocument() }

  private fun parse(content: String): Result<OpenAPI> {
    val parseResult = OpenAPIV3Parser().readContents(content, emptyList(), ParseOptions().apply { isResolve = true })
    return when {
      parseResult == null               -> failure("Failed to parse OpenAPI 3 Document")
      parseResult.messages.isNotEmpty() -> failure(*parseResult.messages.toTypedArray())
      else                              -> success(parseResult.openAPI)
    }
  }

  private fun String.isUrl() =
    this.lowercase().startsWith("http://") || this.lowercase().startsWith("https://")

  private fun String.toUrl(): Result<URL> =
    try {
      success(URI(this).toURL())
    } catch (_: Exception) {
      failure("Invalid URL: the URL '$this' is malformed")
    }

  private fun URL.loadOpenApiDocument(): Result<String> {
    val connection = openConnection() as? HttpURLConnection
                     ?: return failure("Invalid URL: unable to open HTTP connection for URL: $this")

    connection.requestMethod = "GET"
    connection.connectTimeout = 3_000
    connection.readTimeout = 3_000
    connection.instanceFollowRedirects = true
    return try {
      when (val responseCode = connection.responseCode) {
        in 200..299 -> success(connection.inputStream.bufferedReader().use { it.readText() })
        in 400..499 -> failure("Client error: $responseCode ${connection.responseMessage} when fetching OpenAPI Document from URL: $this")
        in 500..599 -> failure("Server error: $responseCode ${connection.responseMessage} when fetching OpenAPI Document from URL: $this")
        else        -> failure("Unexpected response: $responseCode ${connection.responseMessage} when fetching OpenAPI Document from URL: $this")
      }
    } catch (_: SocketTimeoutException) {
      failure("Request timed out: $this")
    } catch (exception: ConnectException) {
      failure("Connection exception for '$this': $exception")
    } catch (exception: Exception) {
      failure("Unexpected exception for '$this': $exception")
    } finally {
      connection.disconnect()
    }
  }

  private fun checkFor2xxResponses(openAPI: OpenAPI): Result<OpenAPI> =
    openAPI.paths
      .flatMap { (path, item) ->
        item.readOperationsMap()
          .filter { (_, operation) -> operation.responses.none { it.key.startsWith("2") } }
          .map { "'${it.key} ${path}: ' does not contain any response for 2xx" }
      }.let {
        if (it.isEmpty()) success(openAPI)
        else failure(*it.toTypedArray())
      }
}
