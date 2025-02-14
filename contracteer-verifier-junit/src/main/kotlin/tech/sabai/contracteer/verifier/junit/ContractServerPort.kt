package tech.sabai.contracteer.verifier.junit

/**
 * Identifies a field holding the **actual** port on which the server is running, primarily useful
 * when the server is started on a random (ephemeral) port. If the annotated field’s value is non-zero,
 * it overrides the port defined in [ContractTest.serverPort].
 **/
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ContractServerPort