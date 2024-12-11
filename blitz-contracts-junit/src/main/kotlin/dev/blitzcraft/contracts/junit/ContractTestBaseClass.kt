package dev.blitzcraft.contracts.junit

import dev.blitzcraft.contracts.core.loader.loadOpenApiSpec
import dev.blitzcraft.contracts.verifier.ServerVerifier
import org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.lang.System.lineSeparator
import java.nio.file.Path

abstract class ContractTestBaseClass {

  protected var serverPort: Int = 8080
  protected var serverUrl: String = "http://localhost"
  protected lateinit var openApiSpecPath: String

  @TestFactory
  fun contractTestsFactory(): List<DynamicTest> {
    val loadingResult = Path.of(openApiSpecPath).loadOpenApiSpec()
    if (loadingResult.hasErrors()) {
      throw IllegalArgumentException(
        "Failed to load OpenAPI spec file:${lineSeparator()}" + loadingResult.errors.joinToString(
          prefix = "- ",
          separator = lineSeparator()+"- ")
      )
    }
    val serverVerifier = ServerVerifier(serverUrl, serverPort)
    return loadingResult.contracts.map {
      DynamicTest.dynamicTest("Validate ${it.description()}") {
        val result = serverVerifier.verify(it)
        if (result.isSuccess().not()) {
          assertionFailure()
            .reason(result.errors().joinToString(
              prefix = lineSeparator(),
              separator = lineSeparator(),
              postfix = lineSeparator()))
            .buildAndThrow()
        }
      }
    }
  }
}