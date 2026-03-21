# contracteer-verifier

Verify that a running server implements its OpenAPI specification.
Use this module when you need programmatic control -- for Kotest, TestNG, or a custom test harness.

If you use JUnit 5, consider [contracteer-verifier-junit](../contracteer-verifier-junit/) for a simpler annotation-based setup.

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

```kotlin
val result = OpenApiLoader.loadOperations("classpath:openapi.yaml")
if (result.isFailure()) {
    fail("Failed to load spec: ${result.errors()}")
}

val cases = result.value!!.flatMap { VerificationCaseFactory.create(it) }

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

## Documentation

See [Verify Your API Programmatically](https://sabai-tech.github.io/contracteer/latest/getting-started/verifier/) for the full guide -- result interpretation, test data preparation, and debugging.