package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.dsl.allOfType
import tech.sabai.contracteer.core.dsl.anyOfType
import tech.sabai.contracteer.core.dsl.numberType
import tech.sabai.contracteer.core.dsl.objectType
import tech.sabai.contracteer.core.dsl.oneOfType
import tech.sabai.contracteer.core.dsl.stringType
import kotlin.test.Test

class DiscriminatorTest {

  @Test
  fun `validation fails when datatype is not fully structured`() {
    // given
    val discriminator = Discriminator("type")

    // when
    val result = discriminator.validate(stringType())

    // then
    assert(result.isFailure())
    assert(result.errors().first().contains("Invalid schema for discriminator"))
  }

  @Test
  fun `validation succeeds when discriminator property exists but is not declared as required`() {
    // given
    val discriminator = Discriminator("type")
    val objectDataType = objectType {
      properties { "type" to stringType() }
    }

    // when
    val result = discriminator.validate(objectDataType)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validation fails when object does not contain discriminator property`() {
    // given
    val discriminator = Discriminator("type")
    val objectDataType = objectType {
      properties { "prop1" to stringType() }
      required("prop1")
    }

    // when
    val result = discriminator.validate(objectDataType)

    // then
    assert(result.isFailure())
    assert(result.errors().first().contains("'type' must be defined on the schema"))
  }

  @Test
  fun `validation fails when discriminator property is not of type string`() {
    // given
    val discriminator = Discriminator("type")
    val objectDataType = objectType {
      properties { "type" to numberType() }
      required("type")
    }

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
    val objectDataType = objectType {
      properties { "type" to stringType() }
      required("type")
    }

    // when
    val result = discriminator.validate(objectDataType)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validations fails when AllOfDataType has duplicated discriminator property`() {
    // given
    val discriminator = Discriminator("type")
    val allOfDataType = allOfType {
      subType(objectType {
        properties { "type" to stringType() }
        required("type")
      })
      subType(objectType {
        properties { "type" to stringType() }
        required("type")
      })
    }

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
    val allOfDataType = allOfType {
      subType(objectType {
        properties { "type" to stringType() }
        required("type")
      })
      subType(objectType {
        properties { "prop" to stringType() }
        required("prop")
      })
    }

    // when
    val result = discriminator.validate(allOfDataType)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validations fails when one sub datatype of AnyOfDataType does not contain discriminator property`() {
    // given
    val discriminator = Discriminator("type")
    val anyOfDataType = anyOfType {
      subType(objectType {
        properties { "type" to stringType() }
        required("type")
      })
      subType(objectType {
        properties { "prop" to stringType() }
        required("prop")
      })
    }

    // when
    val result = discriminator.validate(anyOfDataType)

    // then
    assert(result.isFailure())
    assert(result.errors().first().contains("'type' must be defined on the schema"))
  }

  @Test
  fun `validations succeeds when all sub datatype of AnyOfDataType contain discriminator property`() {
    // given
    val discriminator = Discriminator("type")
    val anyOfDataType = anyOfType {
      subType(objectType {
        properties { "type" to stringType() }
        required("type")
      })
      subType(objectType {
        properties { "type" to stringType() }
        required("type")
      })
    }

    // when
    val result = discriminator.validate(anyOfDataType)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validations fails when one sub datatype of OneOfDataType does not contain discriminator property`() {
    // given
    val discriminator = Discriminator("type")
    val oneOfDataType = oneOfType {
      subType(objectType {
        properties { "type" to stringType() }
        required("type")
      })
      subType(objectType {
        properties { "prop" to stringType() }
        required("prop")
      })
    }

    // when
    val result = discriminator.validate(oneOfDataType)

    // then
    assert(result.isFailure())
    assert(result.errors().first().contains("'type' must be defined on the schema"))
  }

  @Test
  fun `validations succeeds when all sub datatype of OneOfDataType contain discriminator property`() {
    // given
    val discriminator = Discriminator("type")
    val oneOfDataType = oneOfType {
      subType(objectType {
        properties { "type" to stringType() }
        required("type")
      })
      subType(objectType {
        properties { "type" to stringType() }
        required("type")
      })
    }

    // when
    val result = discriminator.validate(oneOfDataType)

    // then
    assert(result.isSuccess())
  }
}
