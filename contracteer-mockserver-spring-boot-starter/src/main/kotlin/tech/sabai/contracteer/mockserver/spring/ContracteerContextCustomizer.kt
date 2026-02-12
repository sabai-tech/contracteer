package tech.sabai.contracteer.mockserver.spring

import tech.sabai.contracteer.mockserver.MockServer
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ContextClosedEvent
import org.springframework.test.context.ContextCustomizer
import org.springframework.test.context.MergedContextConfiguration
import tech.sabai.contracteer.core.swagger.OpenApiLoader

internal class ContracteerContextCustomizer(
  private val mockServerAnnotations: List<ContracteerMockServer>): ContextCustomizer {

  override fun customizeContext(context: ConfigurableApplicationContext, mergedConfig: MergedContextConfiguration) {
    mockServerAnnotations.forEach { startMockServer(context, it) }
  }

  private fun startMockServer(context: ConfigurableApplicationContext, annotation: ContracteerMockServer) {
    val operationsResult = OpenApiLoader.loadOperations(annotation.openApiDoc)
    if (operationsResult.isFailure())
      throw IllegalArgumentException("Error while loading Operations: ${System.lineSeparator()}" + operationsResult
        .errors()
        .joinToString(System.lineSeparator()))

    val mockServer = MockServer(operationsResult.value!!, annotation.port)
    mockServer.start()

    TestPropertyValues.of(
      "${annotation.baseUrlProperty}=http://localhost:${mockServer.port()}",
      "${annotation.portProperty}=${mockServer.port()}")
      .applyTo(context.environment)

    context.addApplicationListener {
      if (it is ContextClosedEvent) {
        mockServer.stop()
      }
    }
  }
}