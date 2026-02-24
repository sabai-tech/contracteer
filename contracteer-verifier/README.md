# contracteer-verifier

Verify that a running server implements its OpenAPI
specification.

## When to use this module

Use contracteer-verifier when you want programmatic control
over contract verification -- for instance in Kotest, TestNG,
or a custom test harness. It loads an OpenAPI specification,
generates verification cases, and runs them against your server.

If you use JUnit 5, consider
[contracteer-verifier-junit](../contracteer-verifier-junit/)
for a simpler annotation-based setup.

## Dependency

Gradle (Kotlin DSL):

```kotlin
dependencies {
    testImplementation("tech.sabai.contracteer:contracteer-verifier:<version>")
}
```

Maven:

```xml
<dependency>
    <groupId>tech.sabai.contracteer</groupId>
    <artifactId>contracteer-verifier</artifactId>
    <version>${contracteer.version}</version>
    <scope>test</scope>
</dependency>
```

## Usage

Three steps: load the spec, generate verification cases, run
them against your server.

```kotlin
val result = OpenApiLoader.loadOperations("openapi.yaml")
if (result.isFailure()) {
    // handle errors: result.errors()
}

val cases = result.value!!.flatMap { VerificationCaseFactory.create(it) }

val verifier = ServerVerifier(ServerConfiguration(
    baseUrl = "http://localhost",
    port = 8080
))

for (case in cases) {
    val outcome = verifier.verify(case)
    // outcome.case.displayName  — human-readable test name
    // outcome.result            — success or failure with errors
}
```

## What the verifier checks

For each verification case, the verifier sends a request to
your server and validates the response:

- **Status code** must match the expected value.
- **Headers** must be present and conform to their declared
  types.
- **Body** must match the response schema's type and structure.

The verifier checks **schema conformance, not value equality**.
Your server is free to return any values that satisfy the
schema -- the verifier does not compare response values against
the OpenAPI examples.

## Verification case types

`VerificationCaseFactory.create()` generates three kinds of
cases from each operation:

- **Scenario-based** -- one case per scenario defined in the
  OpenAPI specification via the `examples` keyword. Uses the
  example values for the request and validates the response
  against the corresponding status code's schema.
- **Schema-based** -- generated when no scenario targets a 2xx
  response. Sends a request with random schema-conforming
  values and validates the 2xx response.
- **Type-mismatch** -- generated when a 400 response is defined.
  Sends intentionally malformed requests (wrong type for a
  parameter or body) and validates that the server returns 400.

## Debugging

When a verification fails, Contracteer logs the HTTP request
and response at WARN level automatically.

To see all HTTP traffic (including successful verifications),
set the `tech.sabai.contracteer.http` logger to DEBUG in your
logging framework.
