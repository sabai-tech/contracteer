# contracteer-mockserver-spring-boot-starter

Spring Boot test integration for the Contracteer mock server.

## Dependency

Gradle (Kotlin DSL):

```kotlin
dependencies {
    testImplementation("tech.sabai.contracteer:contracteer-mockserver-spring-boot-starter:<version>")
}
```

Maven:

```xml
<dependency>
    <groupId>tech.sabai.contracteer</groupId>
    <artifactId>contracteer-mockserver-spring-boot-starter</artifactId>
    <version>${contracteer.version}</version>
    <scope>test</scope>
</dependency>
```

## Usage

Annotate your test class with `@ContracteerMockServer`.
The mock server starts automatically with the Spring test
context and stops when the context closes.

Kotlin:

```kotlin
@SpringBootTest
@ContracteerMockServer(
    openApiDoc = "src/test/resources/openapi.yaml",
    portProperty = "api.port"
)
class MyClientTest {

    @Value("\${api.port}")
    private lateinit var mockServerPort: String

    @Test
    fun `client calls the API`() {
        // HTTP requests to http://localhost:$mockServerPort/...
    }
}
```

Java:

```java
@SpringBootTest
@ContracteerMockServer(
    openApiDoc = "src/test/resources/openapi.yaml",
    portProperty = "api.port"
)
class MyClientTest {

    @Value("${api.port}")
    String mockServerPort;

    @Test
    void clientCallsTheApi() {
        // HTTP requests to http://localhost:{mockServerPort}/...
    }
}
```

The `portProperty` value defines the Spring property where the
mock server port is injected. Use it to configure your client
to point at the mock server.

## Configuration

| Parameter | Default | Description |
|-----------|---------|-------------|
| `openApiDoc` | (required) | Path or URL to the OpenAPI 3 document. |
| `port` | `0` | Server port. `0` assigns a random available port. |
| `portProperty` | `"contracteer.mockserver.port"` | Spring property name where the actual port is injected. |
| `baseUrlProperty` | `"contracteer.mockserver.baseUrl"` | Spring property name where the base URL is injected (format: `http://localhost:{port}`). |

## Multiple mock servers

The annotation is repeatable. Use it to start one mock server
per API your client depends on:

```kotlin
@SpringBootTest
@ContracteerMockServer(
    openApiDoc = "src/test/resources/billing-api.yaml",
    portProperty = "billing.api.port"
)
@ContracteerMockServer(
    openApiDoc = "src/test/resources/inventory-api.yaml",
    portProperty = "inventory.api.port"
)
class MyClientTest { }
```

## How the mock server handles requests

For each incoming request, the mock server evaluates three
steps in order: request validation against the schema, scenario
matching against OpenAPI `examples`, and schema-only response
generation as a fallback. If the request is invalid and the
operation defines a 400 response, the server returns 400. If
the mock server cannot determine the correct response, it
returns 418 with diagnostic information explaining why. See
[contracteer-mockserver](../contracteer-mockserver/) for
details.
