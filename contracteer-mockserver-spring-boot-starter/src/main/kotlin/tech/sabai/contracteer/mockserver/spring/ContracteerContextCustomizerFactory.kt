package tech.sabai.contracteer.mockserver.spring

import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.test.context.ContextConfigurationAttributes
import org.springframework.test.context.ContextCustomizer
import org.springframework.test.context.ContextCustomizerFactory


internal class ContracteerContextCustomizerFactory: ContextCustomizerFactory {
  override fun createContextCustomizer(testClass: Class<*>,
                                       configAttributes: MutableList<ContextConfigurationAttributes>): ContextCustomizer? {
    val annotations = getContracteerContractsMokServerAnnotations(testClass)
    return if (annotations.isNotEmpty()) ContracteerContextCustomizer(annotations) else null
  }

  private fun getContracteerContractsMokServerAnnotations(testClass: Class<*>): List<ContracteerMockServer> =
    AnnotatedElementUtils.findMergedRepeatableAnnotations(testClass, ContracteerMockServer::class.java).toList()
}