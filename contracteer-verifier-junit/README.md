# contracteer-verifier-junit

JUnit 5 integration for contract verification against an
OpenAPI specification.

## Dependency

Gradle (Kotlin DSL):

```kotlin
dependencies {
    testImplementation("tech.sabai.contracteer:contracteer-verifier-junit:<version>")
}
```

Maven:

```xml
<dependency>
    <groupId>tech.sabai.contracteer</groupId>
    <artifactId>contracteer-verifier-junit</artifactId>
    <version>${contracteer.version}</version>
    <scope>test</scope>
</dependency>
```

## Usage

Annotate a test method with `@ContracteerTest`. Contracteer
reads the OpenAPI specification, generates one verification
case per contract, and runs them as individual JUnit tests.

```kotlin
class MyApiContractTest {

    @ContracteerTest(
        openApiDoc = "src/test/resources/openapi.yaml",
        serverUrl = "http://localhost",
        serverPort = 8080
    )
    fun `verify API contracts`() {
        // This runs before each verification case.
        // Use it to prepare test data or start services.
    }
}
```

The method body executes before each verification case. After
it returns, Contracteer sends the request and validates the
response.

`openApiDoc` accepts a file path, an HTTP(S) URL, or a
classpath resource (e.g. `classpath:openapi.yaml`).

## Dynamic server port

When your server starts on a random port, use
`@ContracteerServerPort` on a field to capture the actual port.
If the field value is non-zero, it overrides `serverPort`.

```kotlin
class MyApiContractTest {

    companion object {
        @field:ContracteerServerPort
        private var serverPort: Int = 0

        @JvmStatic
        @BeforeAll
        fun startServer() {
            // Start server on random port, assign to serverPort
        }
    }

    @ContracteerTest(
        openApiDoc = "src/test/resources/openapi.yaml"
    )
    fun `verify API contracts`() { }
}
```

## Spring Boot example

A typical Spring Boot setup in Java, using `@LocalServerPort`
to wire the random port into Contracteer:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MyApiContractTest {

    @ContracteerServerPort
    @LocalServerPort
    int serverPort;

    @ContracteerTest(openApiDoc = "src/test/resources/openapi.yaml")
    void verifyApiContracts() { }
}
```

## What gets verified

Each verification case sends a request to your server and
validates the response:

- **Status code** must match the expected value.
- **Headers** must be present and conform to their declared
  types.
- **Body** must match the response schema's type and structure.

The verifier checks schema conformance, not value equality.
Your server is free to return any values that satisfy the
schema.

Contracteer generates three kinds of verification cases from
each operation: scenario-based (from OpenAPI `examples`),
schema-based (random values when no scenario exists), and
type-mismatch (intentionally malformed requests when a 400
response is defined). See
[contracteer-verifier](../contracteer-verifier/) for details.

## Debugging

When a verification case fails, Contracteer logs the HTTP
request and response at WARN level automatically.

To see all HTTP traffic (including successful cases), set the
`tech.sabai.contracteer.http` logger to DEBUG in your logging
framework.
