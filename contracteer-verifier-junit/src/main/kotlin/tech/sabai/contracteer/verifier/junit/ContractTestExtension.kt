package tech.sabai.contracteer.verifier.junit

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import tech.sabai.contracteer.core.swagger.OpenApiLoader
import java.lang.System.lineSeparator
import java.util.stream.Stream

internal class ContractTestExtension: TestTemplateInvocationContextProvider {

  override fun supportsTestTemplate(context: ExtensionContext) =
    context.requiredTestMethod.getAnnotation(ContractTest::class.java) != null

  override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
    val annotation = context.requiredTestMethod.getAnnotation(ContractTest::class.java)
                     ?: throw IllegalStateException("Missing @ContractTest on test method")

    val contractsResult = OpenApiLoader.loadContracts(annotation.openApiPath)

    if (contractsResult.isFailure()) {
      throw IllegalArgumentException(
        "Failed to load OpenAPI spec file:${lineSeparator()}" +
        contractsResult.errors().joinToString(prefix = "- ", separator = "${lineSeparator()}- "))
    }

    return contractsResult.value!!.stream().map {
      ContractTestInvocationContext(it)
    }
  }
}