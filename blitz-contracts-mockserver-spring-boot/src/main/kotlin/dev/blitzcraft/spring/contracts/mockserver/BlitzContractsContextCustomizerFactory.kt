package dev.blitzcraft.spring.contracts.mockserver

import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.test.context.ContextConfigurationAttributes
import org.springframework.test.context.ContextCustomizer
import org.springframework.test.context.ContextCustomizerFactory


internal class BlitzContractsContextCustomizerFactory: ContextCustomizerFactory {
  override fun createContextCustomizer(testClass: Class<*>,
                                       configAttributes: MutableList<ContextConfigurationAttributes>): ContextCustomizer? {
    val annotations = getBlitzContractsMokServerAnnotations(testClass)
    return if (annotations.isNotEmpty()) BlitzContractsContextCustomizer(annotations) else null
  }

  private fun getBlitzContractsMokServerAnnotations(testClass: Class<*>): List<BlitzContractsMockServer> =
    AnnotatedElementUtils.findMergedRepeatableAnnotations(testClass, BlitzContractsMockServer::class.java).toList()
}