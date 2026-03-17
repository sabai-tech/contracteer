# Verify Your API Programmatically

Use the verifier library directly when you need programmatic control over contract verification.
This is the right choice for Kotest, TestNG, a custom test harness, or any setup where `@ContracteerTest` does not fit.

If you use JUnit 5, consider [Verifier (JUnit 5)](verifier-junit.md) for a simpler annotation-based setup.

The [contracteer-examples](https://github.com/sabai-tech/contracteer-examples) repository contains complete working projects.

---

## Prerequisites

- JDK 21 or later
- Gradle or Maven
- An OpenAPI 3.0 specification (`.yaml` or `.json`)

---

## Add the Dependency

=== "Gradle (Kotlin DSL)"

    ```kotlin
    dependencies {
        testImplementation("tech.sabai.contracteer:contracteer-verifier:<version>")
    }
    ```

=== "Maven"

    ```xml
    <dependency>
        <groupId>tech.sabai.contracteer</groupId>
        <artifactId>contracteer-verifier</artifactId>
        <version>${contracteer.version}</version>
        <scope>test</scope>
    </dependency>
    ```

---

## Load, Generate, Verify

Three steps: load the specification, generate verification cases, run them against your server.

=== "Kotlin"

    ```kotlin
    // 1. Load the OpenAPI specification
    val result = OpenApiLoader.loadOperations("classpath:openapi.yaml")
    if (result.isFailure()) {
        fail("Failed to load spec: ${result.errors()}")
    }

    // 2. Generate verification cases
    val cases = result.value!!.flatMap { VerificationCaseFactory.create(it) }

    // 3. Verify each case against the server
    val verifier = ServerVerifier(ServerConfiguration(
        baseUrl = "http://localhost",
        port = 8080
    ))

    val failures = cases
        .map { verifier.verify(it) }
        .filter { it.result.isFailure() }

    assertThat(failures)
        .withFailMessage {
            failures.joinToString("\n") {
                "${it.case.displayName}: ${it.result.errors()}"
            }
        }
        .isEmpty()
    ```

=== "Java"

    ```java
    // 1. Load the OpenAPI specification
    var result = OpenApiLoader.loadOperations("classpath:openapi.yaml");
    if (result.isFailure()) {
        fail("Failed to load spec: " + result.errors());
    }

    // 2. Generate verification cases
    var cases = result.getValue().stream()
        .flatMap(op -> VerificationCaseFactory.create(op).stream())
        .toList();

    // 3. Verify each case against the server
    var verifier = new ServerVerifier(new ServerConfiguration(
        "http://localhost",
        8080
    ));

    var failures = cases.stream()
        .map(verifier::verify)
        .filter(outcome -> outcome.getResult().isFailure())
        .toList();

    assertThat(failures)
        .withFailMessage(() ->
            failures.stream()
                .map(f -> f.getCase().getDisplayName() + ": " + f.getResult().errors())
                .collect(Collectors.joining("\n")))
        .isEmpty();
    ```

`OpenApiLoader.loadOperations()` accepts a file path, an HTTP(S) URL, or a classpath resource (e.g., `classpath:openapi.yaml`).

`ServerConfiguration` fields:

- **`baseUrl`** *(default: `http://localhost`)* -- Base URL of the server under test.
- **`port`** *(default: `8080`)* -- Port of the server under test.

!!! tip "Treat the specification as a shared artifact"
    Contracteer encourages [specification-driven contract testing](../concepts/contract-testing.md#the-specification-as-source-of-truth): the OpenAPI specification exists independently of both server and client.
    Package it as a Maven or Gradle dependency and reference it with `classpath:openapi.yaml`.
    This ensures that the server, client, and contract tests all use the same specification.
    The [contracteer-examples](https://github.com/sabai-tech/contracteer-examples) repository demonstrates this pattern with the `musketeer-spec` module.

---

## Interpret the Results

Each call to `verifier.verify()` returns a `VerificationOutcome` with two fields:

**`case`** -- The verification case that was executed.
`case.displayName` provides a human-readable description suitable for test output:
`GET /musketeers/{id} -> 200 (application/json) with scenario 'ATHOS'`

**`result`** -- The validation result.
`result.isSuccess()` returns `true` if the response matches the expected schema.
`result.errors()` returns a list of error messages when validation fails.

---

## Prepare Test Data

Before each verification case, your server must have the right data to return the expected responses.

### Clear before each case

Verification cases may modify data.
A `POST` creates a resource, a `DELETE` removes one.
Clearing and re-seeding before each case ensures every case starts from the same known state.

### Match request examples, not response values

The seeded IDs must match the OpenAPI example values.
If the `ATHOS` scenario sends `GET /musketeers/1`, a musketeer with id `1` must exist.
If no musketeer with that ID exists, the server returns `404` instead of the expected `200`.

The verifier checks schema conformance, not value equality.
If Athos existed with a different weapon, the test would still pass.
What matters is that the right resources exist at the right IDs so the server returns the expected status code.

### Operations that create or modify resources

For `POST`, `PUT`, or `DELETE` operations, the seeded data sets up the precondition.
A `POST /musketeers` scenario needs no existing musketeer for the resource being created.
But if the server validates references, the referenced data must exist.
A mission that references musketeers by name, for example, requires those musketeers to be seeded.

---

## What Gets Verified

The verifier generates four kinds of verification cases from each operation:

- **Named scenarios** -- from OpenAPI example keys shared between request and response (e.g., `ATHOS`, `PORTHOS`).
- **Status-code-prefixed scenarios** -- from keys like `404_UNKNOWN_MUSKETEER` that target a specific status code.
- **Automatic type-mismatch** -- Contracteer sends a wrong type (e.g., a string for an integer parameter) and expects a `400`.
- **Schema-only** -- when no examples exist, Contracteer generates random values and validates the response structure.

For each case, the verifier checks the status code, required headers, and response body structure.
It does not check response values.

See [Testing Your Server](../concepts/testing-your-server.md) for a detailed explanation of what the verifier checks.

---

## Debugging Failures

When a verification case fails, Contracteer logs the HTTP request and response at WARN level automatically.
No configuration is needed -- failed cases are always visible.

To see all HTTP traffic -- including successful cases -- set the `tech.sabai.contracteer.http` logger to DEBUG.

---

## Next Steps

- [Testing Your Server](../concepts/testing-your-server.md) -- what the verifier checks in depth, including automatic 400 testing.
- [Creating Scenarios](../concepts/scenarios.md) -- how to write OpenAPI examples that produce the scenarios you want.
- [Verifier (JUnit 5)](verifier-junit.md) -- annotation-based setup for JUnit 5.
- [contracteer-examples](https://github.com/sabai-tech/contracteer-examples) -- complete working projects with server and client examples.