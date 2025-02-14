package tech.sabai.contracteer.verifier.junit

import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.extension.*
import tech.sabai.contracteer.core.contract.Contract
import tech.sabai.contracteer.verifier.ServerConfiguration
import tech.sabai.contracteer.verifier.ServerVerifier
import java.lang.System.lineSeparator
import java.lang.reflect.Method
import java.lang.reflect.Modifier

internal class ContractTestInvocationContext(
  private val contract: Contract): TestTemplateInvocationContext {

  private val logger = KotlinLogging.logger {}

  override fun getDisplayName(invocationIndex: Int) =
    "Contract: ${contract.description()}"

  override fun getAdditionalExtensions(): List<Extension> {
    return listOf(
      object: InvocationInterceptor {
        override fun interceptTestTemplateMethod(invocation: InvocationInterceptor.Invocation<Void>,
                                                 context: ReflectiveInvocationContext<Method>,
                                                 extensionContext: ExtensionContext) {
          val annotation = extensionContext.requiredTestMethod.getAnnotation(ContractTest::class.java)
                           ?: throw IllegalStateException("Missing @ContractTest on test method")

          // Execute the user's test method body (e.g. to set up mocks or prepare the server)
          invocation.proceed()

          val annotatedPort = extractAnnotatedServerPort(extensionContext)
          val actualServerPort =
            if (annotatedPort == null || annotatedPort == 0) {
              logger.debug { "No valid annotated server port found; using fallback port ${annotation.serverPort}" }
              annotation.serverPort
            } else {
              logger.debug { "Using annotated server port: $annotatedPort" }
              annotatedPort
            }
          val configuration = ServerConfiguration(annotation.serverUrl, actualServerPort)
          val testResult = ServerVerifier(configuration).verify(contract)

          if (testResult.isFailure())
            fail<Unit>(testResult.errors().joinToString(prefix = lineSeparator(), separator = lineSeparator()))
        }
      }
    )
  }

  private fun extractAnnotatedServerPort(extensionContext: ExtensionContext): Int? {
    val testClass = extensionContext.requiredTestClass
    val portField = testClass.declaredFields.find { it.isAnnotationPresent(ContractServerPort::class.java) }

    return if (portField != null) {
      portField.isAccessible = true
      val value = when {
        Modifier.isStatic(portField.modifiers) -> portField.getInt(null)
        else                                   -> portField.get(extensionContext.requiredTestInstance)
      }
      logger.debug { "Found @ContractServerPort on field '${portField.name}' with value: $value" }
      value as? Int
    } else {
      logger.debug { "No field annotated with @ContractServerPort found in ${testClass.name}" }
      null
    }
  }
}

