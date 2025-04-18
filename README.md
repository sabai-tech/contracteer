# Contracteer

**Contracteer** is a powerful, Kotlin-based toolkit designed for **contract-first API development and testing** using the OpenAPI 3 specification. 
It enables developers to create, verify, and maintain API contracts efficiently, promoting consistency, early detection of integration issues, and streamlined collaboration.

![License](https://img.shields.io/github/license/sabai-tech/contracteer)
![Build Status](https://img.shields.io/github/actions/workflow/status/sabai-tech/contracteer/release.yml?branch=main)
![Latest Release](https://img.shields.io/github/v/release/sabai-tech/contracteer)
![Maven Central](https://img.shields.io/maven-central/v/tech.sabai.contracteer/contracteer-core?label=Maven%20Central)

## ✨ Why Contracteer?

- **Fast, Reliable, and Isolated Tests**: Quickly verify API contracts with the speed and reliability of unit tests.
- **OpenAPI-Based**: Leverage the widely adopted OpenAPI 3 specification.
- **Shift-Left Testing**: Detect API mismatches early, minimizing integration risks.
- **CI/CD Ready**: Seamlessly integrate into continuous integration pipelines.
- **Framework-Friendly**: Deep integration with popular JVM test frameworks (JUnit, Spring Boot).

## 📦 Modules

| Module | Artifact | Description |
|--------|----------|-------------|
| **contracteer-core** | [`contracteer-core`](https://search.maven.org/search?q=g:tech.sabai.contracteer%20a:contracteer-core) | Core functionalities and OpenAPI spec parsing |
| **contracteer-cli** | CLI binary | CLI engine for running mock servers and verifications |
| **contracteer-mockserver** | [`contracteer-mockserver`](https://search.maven.org/search?q=g:tech.sabai.contracteer%20a:contracteer-mockserver) | Mock server generation for contract-based testing |
| **contracteer-mockserver-spring-boot-starter** | [`spring-boot-starter`](https://search.maven.org/search?q=g:tech.sabai.contracteer%20a:contracteer-mockserver-spring-boot-starter) | Seamless mock server integration with Spring Boot tests |
| **contracteer-verifier** | [`contracteer-verifier`](https://search.maven.org/search?q=g:tech.sabai.contracteer%20a:contracteer-verifier) | Automated verification of API responses against contracts |
| **contracteer-verifier-junit** | [`contracteer-verifier-junit`](https://search.maven.org/search?q=g:tech.sabai.contracteer%20a:contracteer-verifier-junit) | JUnit 5 integration for automated contract verification tests |

## 🚀 Quick Start

### CLI Installation

#### Mac OS (via Homebrew)
```bash
brew tap sabai-tech/contracteer
brew install contracteer
```
#### Linux/Windows
Download the latest release zip file from the [Latest Release page](https://github.com/sabai-tech/contracteer/releases/latest)
```bash
curl -LO https://github.com/sabai-tech/contracteer/releases/latest/download/contracteer-linux-x86_64.zip
unzip contracteer-linux-x86_64.zip
cd contracteer-linux-x86_64/bin
```
### CLI Usage
Run Contracteer Mock Server:

```bash
contracteer mockserver openapi.yaml --port 9090
```

Verify API Contracts:

```bash
contracteer verify openapi.yaml --serverUrl http://localhost --serverPort 8080
```

### JUnit Integration

Add to your `build.gradle.kts`
```kotlin
dependencies {
    testImplementation("tech.sabai.contracteer:contracteer-verifier-junit:<version>")
}
```
or to your `pom.xml`
```xml
<dependency>
  <groupId>tech.sabai.contracteer</groupId>
  <artifactId>contracteer-verifier-junit</artifactId>
  <version>${version}</version>
  <scope>test</scope>
</dependency>
```

And quickly integrate contract verification into your tests:

```kotlin
@ContracteerTest(
    openApiPath = "src/test/resources/openapi.yaml",
    serverPort = 9090
)
fun `verify API contracts`() {
  // You can prepare mocks or insert data before each contract test run.
}
```
This annotation will invoke one test per contract defined in the OpenAPI 3 Document.

### Spring Boot Integration

Add to your `build.gradle.kts`
```kotlin
dependencies {
    testImplementation("tech.sabai.contracteer:contracteer-mockserver-spring-boot-starter:<version>")
}
```
or to your `pom.xml`
```xml
<dependency>
    <groupId>tech.sabai.contracteer</groupId>
    <artifactId>contracteer-mockserver-spring-boot-starter</artifactId>
    <version>${version}</version>
    <scope>test</scope>
</dependency>

```

And easily set up a mock server in your Spring Boot test context:

```kotlin
@SpringBootTest
@ContracteerMockServer(
  openApiDoc = "src/main/resources/openapi.yaml",
  portProperty = "spring.property.server.port",
)
class MyClientTest {

    @Value("\${spring.property.server.port}")
    private lateinit var mockServerPort: String

    // Your tests here
}
```
This starts the mock server before tests and injects the dynamic port into the Spring context.

## 📚 Full Documentation

Explore more in-depth documentation and examples:

👉 [Contracteer Documentation](https://sabai-tech.github.io/contracteer)

## 🤝 Contributing

We warmly welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for how to get involved.

## 📝 License

Contracteer is licensed under [GNU General Public License v3.0](LICENSE).
