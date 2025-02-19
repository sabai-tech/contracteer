package tech.sabai.contracteer.mockserver.spring

import tech.sabai.contracteer.core.swagger.loadContracts
import tech.sabai.contracteer.mockserver.MockServer
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ContextClosedEvent
import org.springframework.test.context.ContextCustomizer
import org.springframework.test.context.MergedContextConfiguration
import java.io.File

internal class ContracteerContextCustomizer(
  private val contracteerContractsMockServerAnnotations: List<ContracteerMockServer>): ContextCustomizer {

  override fun customizeContext(context: ConfigurableApplicationContext, mergedConfig: MergedContextConfiguration) {

    contracteerContractsMockServerAnnotations.forEach { annotation ->
      val contractsResult = File(annotation.specificationFilePath).loadContracts()
      if (contractsResult.isFailure())
        throw IllegalArgumentException("Error while loading Contracts: ${System.lineSeparator()}" + contractsResult
          .errors()
          .joinToString(System.lineSeparator()))

      val mockServer = MockServer(contractsResult.value!!, annotation.port)
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
}