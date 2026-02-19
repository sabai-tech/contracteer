# Contracteer

![Build Status](https://img.shields.io/github/actions/workflow/status/sabai-tech/contracteer/tests.yml?branch=main)
![Maven Central](https://img.shields.io/maven-central/v/tech.sabai.contracteer/contracteer-core)
![License](https://img.shields.io/github/license/sabai-tech/contracteer)

Contracteer verifies that your API implementation matches your
OpenAPI specification and provides a mock server that behaves
exactly as your spec defines. Your OpenAPI specification is the
single source of truth -- Contracteer turns it into executable
tests and a faithful mock.

## Why Contracteer?

API specifications drift from their implementations. A field gets
renamed, a status code changes, a required parameter becomes
optional -- and nothing catches it until an integration test
fails in staging, or worse, a consumer breaks in production.

Contract tests catch this drift early. They run in the build,
with the speed of unit tests, and verify that the boundary
between services works as documented. They sit between unit
tests and end-to-end tests in the testing pyramid: faster and
more reliable than E2E tests, with more integration confidence
than unit tests alone.

Contracteer is **specification-driven**. Unlike consumer-driven
tools where consumers define their expectations, Contracteer
takes the OpenAPI specification you already have and tests
conformance to it. If your spec includes named examples,
Contracteer uses them as scenarios for targeted, deterministic
testing. If not, it generates values from the schema.

## How It Works

Contracteer provides two tools:

**The verifier** sends requests to your real server and validates
that responses match the OpenAPI specification -- correct status
codes, headers, and body structure. It proves your implementation
honors the contract.

**The mock server** receives requests from your client code,
validates them against the full OpenAPI schema, and returns
spec-compliant responses. It acts as a reference implementation
of your API, catching client-side bugs that looser mocks would
miss.

## Requirements

- Java 21 or higher
- Spring Boot 3.x (for the Spring Boot starter module only)

## Modules

Contracteer is modular. Pick the entry point that fits your
stack.

### Direct use

| Module | Description |
|--------|-------------|
| [contracteer-verifier](contracteer-verifier/) | Verify a running server against an OpenAPI specification. Use this in any JVM project. |
| [contracteer-mockserver](contracteer-mockserver/) | Start a mock server from an OpenAPI specification. Use this in any JVM project. |

### Framework integrations

| Module | Description |
|--------|-------------|
| [contracteer-verifier-junit](contracteer-verifier-junit/) | JUnit 5 integration for the verifier. One annotation, zero plumbing. |
| [contracteer-mockserver-spring-boot-starter](contracteer-mockserver-spring-boot-starter/) | Spring Boot test integration for the mock server. Auto-configured and injected into your test context. |

### CLI

| Module | Description |
|--------|-------------|
| [contracteer-cli](contracteer-cli/) | Run the verifier or mock server from the command line. Works with any language or stack, integrates into CI/CD pipelines. |

### Foundation

| Module | Description |
|--------|-------------|
| [contracteer-core](contracteer-core/) | Core domain model and OpenAPI extraction. Use this if you are building tooling on top of Contracteer. |

## Contributing

Contributions are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md)
for how to get involved.

## License

Contracteer is licensed under the
[GNU General Public License v3.0](LICENSE).
