package dev.blitzcraft.contracts.junit

import dev.blitzcraft.contracts.core.loader.swagger.loadContracts
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
    val result = Path.of(openApiSpecPath).loadContracts()
    if (result.isFailure()) {
      throw IllegalArgumentException(
        "Failed to load OpenAPI spec file:${lineSeparator()}" + result.errors().joinToString(
          prefix = "- ",
          separator = lineSeparator() + "- ")
      )
    }
    val serverVerifier = ServerVerifier(serverUrl, serverPort)
    return result.value!!.map {
      DynamicTest.dynamicTest("Validate ${it.description()}") {
        val testResult = serverVerifier.verify(it)
        if (testResult.isFailure()) {
          assertionFailure()
            .reason(testResult.errors().joinToString(
              prefix = lineSeparator(),
              separator = lineSeparator(),
              postfix = lineSeparator()))
            .buildAndThrow()
        }
      }
    }
  }
}