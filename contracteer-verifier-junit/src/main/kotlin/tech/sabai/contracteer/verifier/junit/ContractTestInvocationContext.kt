package tech.sabai.contracteer.verifier.junit

import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.extension.*
import tech.sabai.contracteer.verifier.ServerVerifier
import tech.sabai.contracteer.verifier.VerificationCase
import java.lang.System.lineSeparator
import java.lang.reflect.Method

internal class ContractTestInvocationContext(
  private val verificationCase: VerificationCase,
  private val verifierProvider: (ExtensionContext) -> ServerVerifier
): TestTemplateInvocationContext {

  override fun getDisplayName(invocationIndex: Int) =
    verificationCase.displayName

  override fun getAdditionalExtensions(): List<Extension> {
    return listOf(
      object: InvocationInterceptor {
        override fun interceptTestTemplateMethod(invocation: InvocationInterceptor.Invocation<Void>,
                                                 context: ReflectiveInvocationContext<Method>,
                                                 extensionContext: ExtensionContext) {
          // Execute the user's test method body (e.g. to set up mocks or prepare the server)
          invocation.proceed()

          val outcome = verifierProvider(extensionContext).verify(verificationCase)

          if (outcome.result.isFailure())
            fail<Unit>(outcome.result.errors().joinToString(prefix = lineSeparator(), separator = lineSeparator()))
        }
      }
    )
  }
}
