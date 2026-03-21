# contracteer-verifier-junit

JUnit 5 integration for contract verification against an OpenAPI specification.
One annotation, zero plumbing.

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

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContractTest {

    @ContracteerServerPort
    @LocalServerPort
    int port;

    @ContracteerTest(openApiDoc = "classpath:openapi.yaml")
    void verifyContracts() {
        // Runs before each verification case.
        // Seed test data here.
    }
}
```

Contracteer reads the specification, generates one JUnit test per verification case, and validates that your server responds as documented.

## Documentation

See [Verify Your API with JUnit 5](https://sabai-tech.github.io/contracteer/latest/getting-started/verifier-junit/) for the full guide -- annotation fields, dynamic ports, test data preparation, and debugging.