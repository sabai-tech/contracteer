# Getting Started

Contracteer integrates into your project in three ways: as a JVM library, as a Spring Boot starter, or as a standalone CLI.
Pick the guide that matches your setup.

---

## Test Your Server

The verifier sends requests to your running server and validates that responses conform to your OpenAPI specification.

- [Verifier](verifier.md) -- programmatic setup for Kotlin or Java projects.
- [Verifier with JUnit 5](verifier-junit.md) -- annotation-based setup with `@ContracteerTest`.

---

## Test Your Client

The mock server validates every incoming request and returns spec-compliant responses, giving your client tests an accurate API to run against.

- [Mock Server](mockserver.md) -- programmatic setup for Kotlin or Java projects.
- [Mock Server with Spring Boot](mockserver-spring.md) -- annotation-based setup with `@ContracteerMockServer`.

---

## Use the CLI

The CLI runs verification and starts mock servers from the command line -- no JVM project required.

- [CLI](cli.md) -- installation and commands.

---

## Prerequisites

All guides assume:

- An OpenAPI 3.0 specification (`.yaml` or `.json`)
- JDK 21 or later (for JVM and Spring Boot guides)
- Gradle or Maven (for JVM and Spring Boot guides)

---

## New to Contract Testing?

- [What Is Contract Testing?](../concepts/contract-testing.md) -- what contract testing is and why it matters.
- [How Contracteer Works](../concepts/how-contracteer-works.md) -- how Contracteer turns your OpenAPI specification into a verifier and a mock server.
