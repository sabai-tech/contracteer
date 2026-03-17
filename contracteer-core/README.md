# contracteer-core

Core domain model and OpenAPI extraction engine for Contracteer.
Use this module if you are building custom tooling on top of Contracteer.

If you want to test your API or start a mock server, use one of the higher-level modules instead:
[contracteer-verifier-junit](../contracteer-verifier-junit/),
[contracteer-mockserver-spring](../contracteer-mockserver-spring/),
or [contracteer-cli](../contracteer-cli/).

## Dependency

Gradle (Kotlin DSL):

```kotlin
dependencies {
    implementation("tech.sabai.contracteer:contracteer-core:<version>")
}
```

Maven:

```xml
<dependency>
    <groupId>tech.sabai.contracteer</groupId>
    <artifactId>contracteer-core</artifactId>
    <version>${contracteer.version}</version>
</dependency>
```

## Entry Point

```kotlin
val result = OpenApiLoader.loadOperations("classpath:openapi.yaml")
```

`OpenApiLoader.loadOperations()` parses an OpenAPI specification and returns the list of operations it defines.
It accepts a file path, an HTTP(S) URL, or a `classpath:` resource.

## Documentation

See the [Contracteer documentation](https://sabai-tech.github.io/contracteer) for concepts, getting started guides, and OpenAPI coverage.