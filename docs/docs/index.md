# Contracteer

*The loyal guard of your API contracts.*

Contracteer turns your OpenAPI specification into executable contract tests and a strict mock server.
It is **specification-driven**: the OpenAPI specification is the single source of truth.
Contracteer verifies that both your server and your client conform to it.

---

## Why Contracteer?

API specifications drift from their implementations.
A field gets renamed, a status code changes, a required parameter becomes optional -- and nothing catches it until production breaks.

Contract tests catch this drift early.
They run in the build, with the speed of unit tests, and verify that the boundary between services works as documented.
Contracteer reads your OpenAPI specification, generates test cases from it, and validates conformance.
No handwritten mocks, no manually maintained test cases.

---

## Quick Start

### Test your server

Verify that your API implementation matches the specification.

- [Verifier (JUnit 5)](getting-started/verifier-junit.md) -- one annotation, zero plumbing.
- [Verifier (programmatic)](getting-started/verifier.md) -- for Kotest, TestNG, or custom harnesses.

### Test your client

Run your client tests against a strict, spec-compliant mock server.

- [Mock Server (Spring Boot)](getting-started/mockserver-spring.md) -- one annotation, auto-configured.
- [Mock Server (programmatic)](getting-started/mockserver.md) -- for non-Spring frameworks.

### Use the CLI

Verify servers and start mock servers from the command line -- any language, any stack.

- [CLI](getting-started/cli.md) -- installation and commands.

---

## Learn the Concepts

- [What Is Contract Testing?](concepts/contract-testing.md) -- the problem, the insight, and where contract tests fit in the testing pyramid.
- [How Contracteer Works](concepts/how-contracteer-works.md) -- the verifier, the mock server, and how they derive behavior from the specification.
- [Creating Scenarios](concepts/scenarios.md) -- how OpenAPI examples become scenarios and verification cases.
- [Testing Your Server](concepts/testing-your-server.md) -- what the verifier checks, automatic 400 testing, and schema-only verification.
- [Testing Your Client](concepts/testing-your-client.md) -- how the mock server validates requests, the 418 diagnostic, and the mock server in your test strategy.

---

## Guides

- [CI/CD Integration](guides/ci-integration.md) -- run contract tests in your pipeline.
- [Troubleshooting](guides/troubleshooting.md) -- common issues and how to resolve them.

---

## Examples

The [contracteer-examples](https://github.com/sabai-tech/contracteer-examples) repository contains complete working projects.
It demonstrates the specification-as-artifact pattern with a shared OpenAPI specification consumed by both server and client.
