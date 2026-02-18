# contracteer-core

Core domain model and OpenAPI extraction engine for Contracteer.

## When to use this module

Use contracteer-core if you are building custom tooling on top of
Contracteer or need programmatic access to the parsed OpenAPI
model. It provides the domain types, the OpenAPI extraction
logic, and the type system -- but no test execution or HTTP
server.

If you want to test your API or start a mock server, use one of
the higher-level modules instead:
[contracteer-verifier-junit](../contracteer-verifier-junit/),
[contracteer-mockserver-spring-boot-starter](../contracteer-mockserver-spring-boot-starter/),
or [contracteer-cli](../contracteer-cli/).

## Dependency

Gradle (Kotlin DSL):

```kotlin
dependencies {
    implementation("tech.sabai.contracteer:contracteer-core:<version>")
}
```

Maven:

```xml
<dependency>
    <groupId>tech.sabai.contracteer</groupId>
    <artifactId>contracteer-core</artifactId>
    <version>${contracteer.version}</version>
</dependency>
```

## Entry point

`OpenApiLoader.loadOperations()` parses an OpenAPI specification
and returns the list of operations it defines:

```kotlin
val result = OpenApiLoader.loadOperations("openapi.yaml")
```

The result is a `Result<List<ApiOperation>>` -- either a
success containing the extracted operations, or a failure with
property-scoped validation errors.

## Domain model

An `ApiOperation` represents one HTTP operation (path + method)
and contains everything extracted from the specification:

- **`RequestSchema`** / **`ResponseSchema`** -- structural
  definitions describing what the operation accepts and returns:
  parameters, bodies, data types, and constraints.
- **`Scenario`** -- a named example-based pairing of request
  values and response values for a specific status code, derived
  from OpenAPI `examples` or `example` keywords.
- **`DataType`** -- a sealed hierarchy representing OpenAPI
  types (string, integer, number, boolean, object, array, oneOf,
  anyOf, allOf). Each type validates values and generates random
  conforming data.
- **`Serde`** -- serialization strategy per content type.
  `JsonSerde` for JSON, `PlainTextSerde` for plain text.
- **`Result`** -- error-handling wrapper used throughout the API.
  Carries either a success value or a list of property-scoped
  errors. Composable with `map`, `flatMap`, and `combineWith`.

## Design notes

- **Sealed types** for `DataType`, `ParameterElement`, and
  `Serde` enable exhaustive pattern matching.
- **`Result` over exceptions** -- all public operations return
  `Result` to propagate validation errors without throwing.
- **Kotlin and Java** -- factory methods are annotated with
  `@JvmStatic` and `@JvmOverloads` for clean Java consumption.
