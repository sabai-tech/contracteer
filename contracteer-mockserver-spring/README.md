# contracteer-mockserver-spring

Spring Boot test integration for the Contracteer mock server.
One annotation, auto-configured.

## Dependency

Gradle (Kotlin DSL):

```kotlin
dependencies {
    testImplementation("dev.contracteer:contracteer-mockserver-spring:<version>")
}
```

Maven:

```xml
<dependency>
    <groupId>dev.contracteer</groupId>
    <artifactId>contracteer-mockserver-spring</artifactId>
    <version>${contracteer.version}</version>
    <scope>test</scope>
</dependency>
```

## Usage

```java
@SpringBootTest
@ContracteerMockServer(
    openApiDoc = "classpath:openapi.yaml",
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

The mock server starts automatically with the Spring test context, validates every request against the OpenAPI schema, and returns spec-compliant responses.

## Documentation

See [Mock an API with Spring Boot](https://contracteer.dev/latest/getting-started/mockserver-spring/) for the full guide -- annotation fields, multiple mock servers, mock server behavior, and debugging.