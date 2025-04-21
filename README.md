# Contracteer
![Build Status](https://img.shields.io/github/actions/workflow/status/sabai-tech/contracteer/tests.yml?branch=main)  
![Maven Central](https://img.shields.io/maven-central/v/tech.sabai.contracteer/contracteer-core)  
![License](https://img.shields.io/github/license/sabai-tech/contracteer)

**Contracteer** is a powerful, Kotlin-based toolkit designed for **contract-first API development and testing** using the OpenAPI 3 specification.
It enables developers to create, verify, and maintain API contracts efficiently, promoting consistency, early detection of integration issues, and streamlined collaboration.

- [✨ Why Contracteer?](#-why-contracteer)
- [📚 Full Documentation](#-full-documentation)
- [🚀 Quick Start](#-quick-start)
    - [JUnit Integration — Verify Your API Implementation](#junit-integration--verify-your-api-implementation)
    - [Spring Boot Integration — Mock an API for Your Client](#spring-boot-integration--mock-an-api-for-your-client)
    - [CLI - Use Contracteer from the Command Line](#cli---use-contracteer-from-the-command-line)
- [🤝 Contributing](#-contributing)
- [📝 License](#-license)


## ✨ Why Contracteer?

- **Fast, Reliable, and Isolated Tests**: Quickly verify API contracts with the speed and reliability of unit tests.
- **OpenAPI-Based**: Leverage the widely adopted OpenAPI 3 specification.
- **Shift-Left Testing**: Detect API mismatches early, minimizing integration risks.
- **CI/CD Ready**: Seamlessly integrate into continuous integration pipelines.
- **Framework-Friendly**: Deep integration with popular JVM test frameworks (JUnit, Spring Boot).

## 📚 Full Documentation

Explore more in-depth documentation and examples:

👉 [Contracteer Documentation](https://sabai-tech.github.io/contracteer)

## 🚀 Quick Start

### JUnit Integration — Verify Your API Implementation

Use this integration when you want to **verify that your application correctly implements the expectations defined in the OpenAPI document**. 
This is useful for testing actual HTTP responses returned by your server against the expected behavior.

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
  // This function acts as the entry point for Contracteer to generate and run tests.
  // You can prepare your test data or mock services here.
}
```
This annotation will invoke one test per contract derived from your OpenAPI 3 document.
If your OpenAPI document includes `example` or `examples` values for requests and responses, Contracteer will use them to drive the tests. This allows you to verify your server implementation against meaningful, documented use cases.

### Spring Boot Integration — Mock an API for Your Client

Use this integration when your application is a client that consumes an API defined by an OpenAPI document. 
This starts a **mock HTTP server that serves responses based on the OpenAPI examples and schemas** defined in your specification. 
It enables you to test your client-side code safely and consistently without requiring the actual backend.

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
The `portProperty` value defines where the mock server port will be injected in your Spring context, allowing your client to connect dynamically.

If your OpenAPI document includes `examples` values for requests and responses, those will be used as the basis for mock responses. If no examples are provided, Contracteer will generate mock data based on the schema definitions.

If you prefer a standalone solution or want to integrate contract testing in your CI/CD pipeline, you can use the Contracteer CLI.

### CLI - Use Contracteer from the Command Line
The Contracteer CLI is a standalone binary ideal for integrating contract verification or mock server startup in your development workflow or CI pipelines.

#### Installation

##### Mac OS (via Homebrew)
```bash
brew tap sabai-tech/contracteer
brew install contracteer
```
##### Linux/Windows
Download the latest release zip file from the [Latest Release page](https://github.com/sabai-tech/contracteer/releases/latest)
```bash
curl -LO https://github.com/sabai-tech/contracteer/releases/latest/download/contracteer-linux-x86_64.zip
unzip contracteer-linux-x86_64.zip
cd contracteer-linux-x86_64/bin
```
#### CLI Usage
Start a mock server on port 9090 using your OpenAPI definition:

```bash
contracteer mockserver openapi.yaml --port 9090
```

Verify that a running server behaves as specified by the operations in the OpenAPI document:

```bash
contracteer verify openapi.yaml --serverUrl http://localhost --serverPort 8080
```

## 🤝 Contributing

We warmly welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for how to get involved.

## 📝 License

Contracteer is licensed under [GNU General Public License v3.0](LICENSE).
