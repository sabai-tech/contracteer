package tech.sabai.contracteer.verifier.junit

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import tech.sabai.contracteer.core.Result.Success
import tech.sabai.contracteer.core.swagger.OpenApiLoader
import tech.sabai.contracteer.verifier.ServerConfiguration
import tech.sabai.contracteer.verifier.ServerVerifier
import tech.sabai.contracteer.verifier.VerificationCaseFactory
import java.lang.System.lineSeparator
import java.lang.reflect.Modifier
import java.util.stream.Stream

internal class ContractTestExtension: TestTemplateInvocationContextProvider {

  override fun supportsTestTemplate(context: ExtensionContext) =
    context.requiredTestMethod.getAnnotation(ContracteerTest::class.java) != null

  override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
    val annotation = context.requiredTestMethod.getAnnotation(ContracteerTest::class.java)
                     ?: throw IllegalStateException("Missing @ContracteerTest on test method")

    val operationsResult = OpenApiLoader.loadOperations(annotation.openApiDoc)

    if (operationsResult !is Success) {
      throw IllegalArgumentException(
        "Failed to load OpenAPI spec file:${lineSeparator()}" +
        operationsResult.errors().joinToString(prefix = "- ", separator = "${lineSeparator()}- "))
    }

    val verifierProvider = createVerifierProvider(annotation)

    val cases: List<TestTemplateInvocationContext> = operationsResult.value
      .flatMap { VerificationCaseFactory.create(it) }
      .map { ContractTestInvocationContext(it, verifierProvider) }

    return cases.stream()
  }

  private fun createVerifierProvider(annotation: ContracteerTest): (ExtensionContext) -> ServerVerifier {
    var cached: ServerVerifier? = null
    return { extensionContext ->
      cached ?: run {
        val port = resolveServerPort(extensionContext, annotation)
        ServerVerifier(ServerConfiguration(annotation.serverUrl, port)).also { cached = it }
      }
    }
  }

  private fun resolveServerPort(extensionContext: ExtensionContext, annotation: ContracteerTest): Int {
    val annotatedPort = extractAnnotatedServerPort(extensionContext)
    return if (annotatedPort == null || annotatedPort == 0) annotation.serverPort else annotatedPort
  }

  private fun extractAnnotatedServerPort(extensionContext: ExtensionContext): Int? {
    val testClass = extensionContext.requiredTestClass
    val portField = testClass.declaredFields.find { it.isAnnotationPresent(ContracteerServerPort::class.java) }
                    ?: return null

    portField.isAccessible = true

    val value = if (Modifier.isStatic(portField.modifiers))
      portField.get(null)
    else
      portField.get(extensionContext.requiredTestInstance)

    return value as? Int
  }
}
