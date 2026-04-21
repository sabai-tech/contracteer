# Test fixtures DSL

Kotlin DSL for constructing `ApiOperation`, `Scenario`, `DataType`, and `ParameterCodec`
fixtures in tests. Shared across `contracteer-core`, `contracteer-verifier`, and
`contracteer-mockserver` via the `java-test-fixtures` plugin.

Add to a downstream module's test classpath:

```kotlin
testImplementation(testFixtures(project(":contracteer-core")))
```

## Operation and scenario

```kotlin
val op = apiOperation("POST", "/users") {
  request {
    pathParam("id", integerType())
    queryParam("verbose", stringType(), isRequired = false)
    header("X-Trace-Id", stringType())
    cookie("session", stringType())
    jsonBody(objectType { properties { "name" to stringType() } })
  }
  response(201) {
    header("Location", stringType())
    jsonBody(objectType { properties { "id" to integerType() } })
  }
  scenario("createUser", status = 201) {
    request {
      pathParam["id"] = 42
      jsonBody { "name" to "Alice" }
    }
    response {
      header["Location"] = "/users/42"
      jsonBody { "id" to 42 }
    }
  }
}
```

Body shorthands: `jsonBody(...)`, `plainTextBody(...)`. Generic form:
`body("application/xml", dataType, PlainTextSerde)`.

## DataType

Scalars are flat factories with named arguments:

```kotlin
stringType(pattern = "\\d{5}", minLength = 5, maxLength = 5)
integerType(minimum = BigDecimal(1), maximum = BigDecimal(100))
```

Composite types use a block builder:

```kotlin
objectType(name = "User", allowAdditionalProperties = false) {
  properties {
    "id" to integerType()
    "name" to stringType()
  }
  required("id", "name")
}

allOfType { subType(petType); subType(catType) }

oneOfType {
  subType(dogType)
  subType(catType)
  discriminator("type") { mapping("DOG", "dog"); mapping("CAT", "cat") }
}

arrayType(items = stringType(), minItems = 1, uniqueItems = true)
```

## Codec

Pass an OAS-named factory to `pathParam`/`queryParam`/`header`/`cookie`:

```kotlin
queryParam("tags", arrayType(items = stringType()), codec = pipeDelimited())
queryParam("filter", oneOfType { ... }, codec = content(JsonSerde))
queryParam("path", stringType(), codec = form(allowReserved = true))
```

Available factories: `simple`, `matrix`, `label`, `form`, `spaceDelimited`,
`pipeDelimited`, `deepObject`, `content`. Defaults match OAS:
`pathParam` → `simple()`, `queryParam` → `form()`, `header` → `simple()`,
`cookie` → `form(explode = false)`.
