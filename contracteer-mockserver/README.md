# contracteer-mockserver

Start a mock server from an OpenAPI specification.

## When to use this module

Use contracteer-mockserver when you want programmatic control
over the mock server -- for instance in a custom test harness
or a non-Spring framework. It starts an HTTP server that
validates requests and returns spec-compliant responses.

If you use Spring Boot, consider
[contracteer-mockserver-spring](../contracteer-mockserver-spring/)
for auto-configured setup.

## Dependency

Gradle (Kotlin DSL):

```kotlin
dependencies {
    testImplementation("tech.sabai.contracteer:contracteer-mockserver:<version>")
}
```

Maven:

```xml
<dependency>
    <groupId>tech.sabai.contracteer</groupId>
    <artifactId>contracteer-mockserver</artifactId>
    <version>${contracteer.version}</version>
    <scope>test</scope>
</dependency>
```

## Usage

Load the spec, create the mock server, start it.

```kotlin
val result = OpenApiLoader.loadOperations("openapi.yaml")
if (result.isFailure()) {
    // handle errors: result.errors()
}

val mockServer = MockServer(
    operations = result.value!!,
    port = 0 // 0 for random port, or a fixed port
)

mockServer.start()
val actualPort = mockServer.port()

// Make HTTP requests to http://localhost:$actualPort/...

mockServer.stop()
```

## How the mock server handles requests

For each incoming request matched to an operation by path and
HTTP method, the mock server evaluates three steps in order:

**1. Request validation** -- validates the request against the
operation's schema (parameters, body, types, required fields).
If invalid and the operation defines a 400 response, the server
returns 400 with a generated body. If invalid and no 400
response is defined, the server returns 418.

**2. Scenario matching** -- compares the request against all
scenarios defined via OpenAPI `examples`. If exactly one
scenario matches, the server returns the scenario's response.
If multiple scenarios match, the server returns 418.

**3. Schema-only response** -- if no scenario matched, the
server generates a response with random values conforming to
the schema. If the operation has a single 2xx response, it uses
that. If multiple 2xx responses exist and the choice is
ambiguous, the server returns 418.

The **418 response** is a diagnostic signal. It means the mock
server received the request but cannot determine the correct
response. The 418 body contains the nearest matching scenarios
and the reason each did not match.
