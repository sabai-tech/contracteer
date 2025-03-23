package tech.sabai.contracteer.core.swagger

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.contract.Contract
import java.io.File
import java.net.*

object OpenApiLoader {

  @JvmStatic
  fun loadContracts(path: String): Result<List<Contract>> =
    path.loadOpenApiDefinition()
      .flatMap { parse(it!!) }
      .flatMap { checkFor2xxResponses(it!!) }
      .flatMap { it!!.generateContracts() }

  private fun String.loadOpenApiDefinition() =
    if (isUrl()) loadFromUrl(this) else loadFromFile(this)

  private fun loadFromFile(path: String): Result<String> {
    val file = File(path)
    return if (file.exists())
      success(file.readText())
    else
      failure("File not found: ${file.absoluteFile}")
  }

  private fun loadFromUrl(path: String) =
    path.toUrl().flatMap { it!!.loadOpenApiDefinition() }

  private fun parse(content: String): Result<OpenAPI> {
    val parseResult = OpenAPIV3Parser().readContents(content, emptyList(), ParseOptions().apply { isResolve = true })
    return when {
      parseResult == null               -> failure("Failed to parse OpenAPI 3 Definition")
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
      failure("Malformed URL: $this")
    }

  private fun URL.loadOpenApiDefinition(): Result<String> {
    val connection = openConnection() as? HttpURLConnection
                     ?: return failure("Unable to open HTTP connection for URL: $this")

    connection.requestMethod = "GET"
    connection.connectTimeout = 3_000
    connection.readTimeout = 3_000
    connection.instanceFollowRedirects = true
    return try {
      when (val responseCode = connection.responseCode) {
        in 200..299 -> success(connection.inputStream.bufferedReader().use { it.readText() })
        in 400..499 -> failure("Client error: $responseCode ${connection.responseMessage}")
        in 500..599 -> failure("Server error: $responseCode ${connection.responseMessage}")
        else        -> failure("Unexpected response: $responseCode ${connection.responseMessage}")
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
          .map { "'${it.key} ${path}: ' does not contain response for 2xx" }
      }.let {
        if (it.isEmpty()) success(openAPI)
        else failure(*it.toTypedArray())
      }
}

