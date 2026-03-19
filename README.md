<p align="center">
<picture>
<source media="(prefers-color-scheme: dark)" srcset="contracteer_black_theme.png">
<img src="contracteer_white_theme.png" alt="Contracteer" width="350">
</picture>
</p>
<h2 align="center">CONTRACTEER</h2>
<p align="center"><i>The loyal guard of your API contracts.</i></p>
<br>

![Build Status](https://img.shields.io/github/actions/workflow/status/sabai-tech/contracteer/tests.yml?branch=main)
![Maven Central](https://img.shields.io/maven-central/v/tech.sabai.contracteer/contracteer-core)
![License](https://img.shields.io/github/license/sabai-tech/contracteer)
[![Documentation](https://img.shields.io/badge/docs-sabai--tech.github.io%2Fcontracteer-blue)](https://sabai-tech.github.io/contracteer)

Contracteer verifies that your API implementation matches your OpenAPI specification and provides a mock server that behaves exactly as your spec defines.
Your OpenAPI specification is the single source of truth -- Contracteer turns it into executable tests and a faithful mock.

## Why Contracteer?

API specifications drift from their implementations.
A field gets renamed, a status code changes, a required parameter becomes optional -- and nothing catches it until a consumer breaks in production.

Contract tests catch this drift early.
They run in the build, with the speed of unit tests, and verify that the boundary between services works as documented.

Contracteer is **specification-driven**.
Unlike consumer-driven tools where consumers define their expectations, Contracteer takes the OpenAPI specification you already have and tests conformance to it.
If your spec includes named examples, Contracteer uses them as scenarios for targeted, deterministic testing.
If not, it generates values from the schema.

## Quick Start

### Verify Your API (JUnit 5)

Add the dependency:

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

Write the test:

```java
class ContractTest {

    @ContracteerTest(
        openApiDoc = "src/test/resources/openapi.yaml",
        serverPort = 8080
    )
    void verifyContracts() {
        // Runs before each verification case.
        // Seed test data here.
    }
}
```

Contracteer reads the specification, generates one JUnit test per verification case, and validates that your server responds as documented.
Works with any server -- Spring Boot, Quarkus, Micronaut, or plain Java.

### Mock an API (Spring Boot)

Add the dependency:

Gradle (Kotlin DSL):

```kotlin
dependencies {
    testImplementation("tech.sabai.contracteer:contracteer-mockserver-spring:<version>")
}
```

Maven:

```xml
<dependency>
    <groupId>tech.sabai.contracteer</groupId>
    <artifactId>contracteer-mockserver-spring</artifactId>
    <version>${contracteer.version}</version>
    <scope>test</scope>
</dependency>
```

Annotate your test:

```java
@SpringBootTest
@ContracteerMockServer(
    openApiDoc = "src/test/resources/openapi.yaml",
    baseUrlProperty = "api.base-url"
)
class MyClientTest {

    @Autowired
    MyApiClient client;

    @Test
    void callsTheApi() {
        var result = client.listProducts();
        assertThat(result).isNotNull();
    }
}
```

The mock server starts automatically, validates every request against the OpenAPI schema, and returns spec-compliant responses.

### CLI

Install:

```bash
brew install sabai-tech/contracteer/contracteer
```

Verify a running server:

```bash
contracteer verify openapi.yaml
```

Start a mock server:

```bash
contracteer mock openapi.yaml
```

The CLI works with any language or stack -- no JVM required.

### Other integrations

Contracteer also provides framework-agnostic libraries for programmatic use:

- [`contracteer-verifier`](contracteer-verifier/) -- verify a server from any JVM test framework.
- [`contracteer-mockserver`](contracteer-mockserver/) -- start a mock server from any JVM project.

All modules:

| Module | Description |
|--------|-------------|
| [`contracteer-core`](contracteer-core/) | Domain model and OpenAPI extraction engine |
| [`contracteer-verifier`](contracteer-verifier/) | Programmatic verifier |
| [`contracteer-verifier-junit`](contracteer-verifier-junit/) | JUnit 5 integration |
| [`contracteer-mockserver`](contracteer-mockserver/) | Programmatic mock server |
| [`contracteer-mockserver-spring`](contracteer-mockserver-spring/) | Spring Boot integration |
| [`contracteer-cli`](contracteer-cli/) | CLI (native binary) |

See the [documentation site](https://sabai-tech.github.io/contracteer) for setup guides.

## Requirements

- Java 21 or higher (for JVM modules)
- Spring Boot 3.x (for the Spring Boot module only)

## Documentation

- [Documentation site](https://sabai-tech.github.io/contracteer) -- concepts, getting started guides, and troubleshooting.
- [contracteer-examples](https://github.com/sabai-tech/contracteer-examples) -- complete working projects demonstrating the specification-as-artifact pattern.

## Contributing

Contributions are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for how to get involved.

## License

Contracteer is licensed under the [GNU General Public License v3.0](LICENSE).
