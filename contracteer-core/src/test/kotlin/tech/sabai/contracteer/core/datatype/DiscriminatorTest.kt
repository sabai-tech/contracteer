package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.TestFixture.allOfDataType
import tech.sabai.contracteer.core.TestFixture.anyOfDataType
import tech.sabai.contracteer.core.TestFixture.numberDataType
import tech.sabai.contracteer.core.TestFixture.objectDataType
import tech.sabai.contracteer.core.TestFixture.oneOfDataType
import tech.sabai.contracteer.core.TestFixture.stringDataType
import kotlin.test.Test

class DiscriminatorTest {

  @Test
  fun `validation fails when datatype is not fully structured`() {
    // given
    val discriminator = Discriminator("type")

    // when
    val result = discriminator.validate(stringDataType())

    // then
    assert(result.isFailure())
    assert(result.errors().first().contains("Invalid schema for discriminator"))
  }

  @Test
  fun `validation fails when discriminator property is not a required property`() {
    // given
    val discriminator = Discriminator("type")
    val objectDataType = objectDataType(
      properties = mapOf("type" to stringDataType()),
    )

    // when
    val result = discriminator.validate(objectDataType)

    // then
    assert(result.isFailure())
    assert(result.errors().first().contains("'type' must be defined as required"))
  }

  @Test
  fun `validation fails when object does not contain discriminator property`() {
    // given
    val discriminator = Discriminator("type")
    val objectDataType = objectDataType(
      properties = mapOf("prop1" to stringDataType()),
      requiredProperties = setOf("prop1")
    )

    // when
    val result = discriminator.validate(objectDataType)

    // then
    assert(result.isFailure())
    assert(result.errors().first().contains("'type' must be defined as required"))
  }

  @Test
  fun `validation fails when discriminator property is not of type string`() {
    // given
    val discriminator = Discriminator("type")
    val objectDataType = objectDataType(
      properties = mapOf("type" to numberDataType()),
      requiredProperties = setOf("type")
    )

    // when
    val result = discriminator.validate(objectDataType)

    // then
    assert(result.isFailure())
    assert(result.errors().first().contains("'type' must be of type 'string'"))
  }

  @Test
  fun `validation succeeds when object has property matching discriminator property name and type`() {
    // given
    val discriminator = Discriminator("type")
    val objectDataType = objectDataType(
      properties = mapOf("type" to stringDataType()),
      requiredProperties = setOf("type")
    )

    // when
    val result = discriminator.validate(objectDataType)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validations fails when AllOfDataType has duplicated discriminator property`() {
    // given
    val discriminator = Discriminator("type")
    val allOfDataType = allOfDataType(
      subTypes = listOf(
        objectDataType(
          properties = mapOf("type" to stringDataType()),
          requiredProperties = setOf("type")
        ),
        objectDataType(
          properties = mapOf("type" to stringDataType()),
          requiredProperties = setOf("type")
        )))

    // when
    val result = discriminator.validate(allOfDataType)

    // then
    assert(result.isFailure())
    assert(result.errors().first().contains("Ambiguous discriminator. Property 'type' "))
  }

  @Test
  fun `validations succeeds when AllOfDataType has only one discriminator property`() {
    // given
    val discriminator = Discriminator("type")
    val allOfDataType = allOfDataType(
      subTypes = listOf(
        objectDataType(
          properties = mapOf("type" to stringDataType()),
          requiredProperties = setOf("type")
        ),
        objectDataType(
          properties = mapOf("prop" to stringDataType()),
          requiredProperties = setOf("prop")
        )))

    // when
    val result = discriminator.validate(allOfDataType)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validations fails when all sub datatype of AnyOfDataType does not contain discriminator property`() {
    // given
    val discriminator = Discriminator("type")
    val anyOfDataType = anyOfDataType(
      subTypes = listOf(
        objectDataType(
          properties = mapOf("type" to stringDataType()),
          requiredProperties = setOf("type")
        ),
        objectDataType(
          properties = mapOf("prop" to stringDataType()),
          requiredProperties = setOf("prop")
        )))

    // when
    val result = discriminator.validate(anyOfDataType)

    // then
    assert(result.isFailure())
    assert(result.errors().first().contains("'type' must be defined as required"))
  }

  @Test
  fun `validations succeeds when all sub datatype of AnyOfDataType contain discriminator property`() {
    // given
    val discriminator = Discriminator("type")
    val anyOfDataType = anyOfDataType(
      subTypes = listOf(
        objectDataType(
          properties = mapOf("type" to stringDataType()),
          requiredProperties = setOf("type")
        ),
        objectDataType(
          properties = mapOf("type" to stringDataType()),
          requiredProperties = setOf("type")
        )))

    // when
    val result = discriminator.validate(anyOfDataType)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validations fails when all sub datatype of OneOfDataType does not contain discriminator property`() {
    // given
    val discriminator = Discriminator("type")
    val oneOfDataType = oneOfDataType(
      subTypes = listOf(
        objectDataType(
          properties = mapOf("type" to stringDataType()),
          requiredProperties = setOf("type")
        ),
        objectDataType(
          properties = mapOf("prop" to stringDataType()),
          requiredProperties = setOf("prop")
        )))

    // when
    val result = discriminator.validate(oneOfDataType)

    // then
    assert(result.isFailure())
    assert(result.errors().first().contains("'type' must be defined as required"))
  }

  @Test
  fun `validations succeeds when all sub datatype of OneOfDataType contain discriminator property`() {
    // given
    val discriminator = Discriminator("type")
    val oneOfDataType = oneOfDataType(
      subTypes = listOf(
        objectDataType(
          properties = mapOf("type" to stringDataType()),
          requiredProperties = setOf("type")
        ),
        objectDataType(
          properties = mapOf("type" to stringDataType()),
          requiredProperties = setOf("type")
        )))

    // when
    val result = discriminator.validate(oneOfDataType)

    // then
    assert(result.isSuccess())
  }
}