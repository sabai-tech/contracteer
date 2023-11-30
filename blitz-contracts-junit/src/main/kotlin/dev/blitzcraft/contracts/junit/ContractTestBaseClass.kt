package dev.blitzcraft.contracts.junit

import dev.blitzcraft.contracts.core.ContractExtractor
import dev.blitzcraft.contracts.verifier.ServerVerifier
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.nio.file.Path

abstract class ContractTestBaseClass {

  protected var serverPort: Int = 8080
  protected var serverUrl: String = "http://localhost"
  protected lateinit var openApiSpecPath: String

  @TestFactory
  fun contractTestsFactory(): List<DynamicTest> {
    val serverVerifier = ServerVerifier(serverUrl, serverPort)
    return ContractExtractor.extractFrom(Path.of(openApiSpecPath)).map {
      DynamicTest.dynamicTest("Validate ${it.description()}") { serverVerifier.verify(it) }
    }
  }
}