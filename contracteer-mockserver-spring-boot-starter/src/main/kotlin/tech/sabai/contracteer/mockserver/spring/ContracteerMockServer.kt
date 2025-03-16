package tech.sabai.contracteer.mockserver.spring

import java.lang.annotation.Inherited

/**
 * Configures a Contracteer Mock Server that serves contract tests based on an OpenAPI 3 Definition.
 *
 * Use this annotation on a class or annotation to indicate that a Contracteer Mock Server should be instantiated and
 * configured to serve responses as defined in the specified OpenAPI 3 Definition file.
 *
 * **Usage Details:**
 * - The server can be configured to listen on either a fixed port or a randomly assigned port.
 * - The OpenAPI 3 Definition file, which may be provided as a local file path or remote URL, determines
 *   the contracts to be served.
 * - Spring properties can be used to inject the actual port and base URL of the mock server into the
 *   application context.
 *
 * @property port The port on which the Contracteer mock server will listen.
 * - A value of `0` indicates that a random available port will be chosen.
 * - A positive value specifies a fixed port.
 *
 * @property openApiPath The file path or URL to the OpenAPI 3 Definition file from which contracts are loaded.
 *
 * @property portProperty The name of the Spring property used to inject the actual port value of the mock server.
 * Default is `"contracteer.mockserver.port"`.
 *
 * @property baseUrlProperty The name of the Spring property used to inject the base URL of the mock server.
 * Default is `"contracteer.mockserver.baseUrl"`.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
@JvmRepeatable(ContracteerMockServers::class)
annotation class ContracteerMockServer(
  val port: Int = 0,
  val openApiPath: String,
  val portProperty: String = "contracteer.mockserver.port",
  val baseUrlProperty: String = "contracteer.mockserver.baseUrl"
)

/**
 * Container annotation for grouping multiple [ContracteerMockServer] annotations.
 *
 * This annotation allows you to configure more than one Contracteer mock server on a single class.
 *
 * @property value An array of [ContracteerMockServer] annotations, each defining a separate mock server configuration.
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class ContracteerMockServers(
  val value: Array<ContracteerMockServer>
)