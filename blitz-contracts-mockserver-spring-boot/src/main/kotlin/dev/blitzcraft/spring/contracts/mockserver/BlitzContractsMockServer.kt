package dev.blitzcraft.spring.contracts.mockserver

import java.lang.annotation.Inherited

/**
 * Annotation for configuring a BlitzContract server, which may be used to serve
 * against an OpenAPI 3 specification.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
@JvmRepeatable(BlitzContractsMockServers::class)
annotation class BlitzContractsMockServer(

  /**
   * The port on which the BlitzContract server will listen.
   *
   * - A value of `0` means a random available port will be chosen.
   * - A value greater than `0` indicates a fixed (static) port will be used.
   *
   * @return the port number
   */
  val port: Int = 0,

  /**
   * The file path to the OpenAPI 3 (OAS 3) specification that this server should reference.
   *
   * @return the path to the OAS 3 specification file
   */
  val specificationFilePath: String,

  /**
   * The name of the Spring property through which the [port] value will be injected.
   *
   * @return the Spring property name for the BlitzContract server port
   */
  val portProperty: String = "blitzcontracts.server.port",

  /**
   * The name of the Spring property through which the base URL of this server will be injected.
   *
   * @return the Spring property name for the BlitzContract server base URL
   */
  val baseUrlProperty: String = "blitzcontracts.server.baseUrl"
)

@MustBeDocumented
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class BlitzContractsMockServers(
  val value: Array<BlitzContractsMockServer>
)