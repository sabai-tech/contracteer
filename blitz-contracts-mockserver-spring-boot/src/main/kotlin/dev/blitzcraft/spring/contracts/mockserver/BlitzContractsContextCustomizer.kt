package dev.blitzcraft.spring.contracts.mockserver

import dev.blitzcraft.contracts.core.loader.swagger.loadContracts
import dev.blitzcraft.contracts.mockserver.MockServer
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ContextClosedEvent
import org.springframework.test.context.ContextCustomizer
import org.springframework.test.context.MergedContextConfiguration
import java.io.File

internal class BlitzContractsContextCustomizer(
  private val blitzContractsMockServerAnnotations: List<BlitzContractsMockServer>): ContextCustomizer {
  private val logger = KotlinLogging.logger {}

  override fun customizeContext(context: ConfigurableApplicationContext, mergedConfig: MergedContextConfiguration) {

    blitzContractsMockServerAnnotations.forEach { annotation ->
      val contractsResult = File(annotation.specificationFilePath).loadContracts()
      if (contractsResult.isFailure())
        throw IllegalArgumentException("Error while loading Contracts: ${System.lineSeparator()}" + contractsResult
          .errors()
          .joinToString(System.lineSeparator()))

      val mockServer = MockServer(contractsResult.value!!, annotation.port)
      mockServer.start()
      logger.info { "Started BlitzContracts Mock server on port ${mockServer.port()} for specification file ${annotation.specificationFilePath}" }

      TestPropertyValues.of(
        "${annotation.baseUrlProperty}=http://localhost:${mockServer.port()}",
        "${annotation.portProperty}=${mockServer.port()}")
        .applyTo(context.environment)

      context.addApplicationListener {
        if (it is ContextClosedEvent) {
          logger.info { "Shutting down Blitz Contracts Mock Server..." }
          mockServer.stop()
        }
      }
    }
  }
}