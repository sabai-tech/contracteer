# What Is Contract Testing?

Contract testing verifies that the boundary between two services works as documented.
It does not test internal logic or end-to-end workflows.
It tests the agreement -- the **contract** -- that both sides depend on.

---

## The Problem: APIs Break at Integration Time

Consider two teams working on the same product.
One team builds a REST API that returns product data.
The other team builds a client that consumes it.

Both teams test their code thoroughly.
The server has unit tests for its business logic.
The client tests against a mock that returns hardcoded responses.
Every test passes.
Every build is green.

Then the server team renames a field from `productName` to `name`.
Their tests still pass -- they test internal behavior, not the HTTP response shape.
The client's tests still pass -- they run against a mock that nobody updated.
The rename ships undetected, and the client breaks in production.

This is **contract drift**: the actual API diverged from what consumers expect, and nothing caught it.
The mock the client relied on became a lie.

This failure mode is not rare.
It happens every time a team changes a response field, adds a required parameter, modifies a status code, or alters an error format.
The cost scales with the number of consumers: each one carries its own stale assumptions about the API.
Even a single team owning both sides is not immune -- the contract can drift without anyone noticing.

---

## The Insight: Test the Boundary, Not the Implementation

The root cause is that each team tests in isolation.
Unit tests verify internal behavior.
End-to-end tests verify the full system.
But neither focuses on the **boundary**.
The boundary is the precise point where one service sends a request and the other returns a response.

A contract test targets exactly this boundary.
It verifies **externally observable guarantees**:

- The status code is what the contract says it should be.
- The response body has the documented structure.
- Required headers are present and correctly typed.
- Invalid requests are rejected with the documented error format.

A contract test does not verify which validation rule fired, what error message was returned, or how the server computed the result.
Those are internal concerns that belong to unit and integration tests.

This distinction is what makes contract tests stable.
Internal behavior changes constantly as teams refactor, optimize, and evolve their code.
The external contract changes rarely -- and when it does, that change should be deliberate, visible, and tested.
As Martin Fowler [puts it][contract-test], contract tests verify format consistency rather than exact data values.
They detect structural contract violations while accommodating legitimate changes in the data itself.

[contract-test]: https://martinfowler.com/bliki/ContractTest.html

---

## Where Contract Tests Fit: The Testing Pyramid

The [testing pyramid][test-pyramid] describes how different test types relate in quantity, speed, and scope.
Higher levels are slower, more expensive, and fewer in number:

```
              /        \
             /   E2E    \
            /            \
           /--------------\
          /                \
         /   Integration    \
        /                    \
       /----------------------\  ◄── Contract tests
      /                        \
     /       Unit Tests         \
    /                            \
   /------------------------------\
```

**Unit tests** form the base.
They are fast, numerous, and test individual components in isolation.
They give no confidence about how components interact across service boundaries.

**Contract tests** are not a traditional layer of the pyramid.
They are a specific type of test that bridges two levels.
They run with the speed of unit tests -- no deployment, no infrastructure -- but they verify integration-level concerns.
Does the server respond as documented?
Does the client send valid requests?

**Integration tests** verify that components work together with real dependencies: databases, message brokers, file systems.
They are slower and require more infrastructure than contract tests.
They cover concerns that contract tests do not -- whether queries return correct data or whether messages are properly consumed.

**End-to-end tests** verify complete workflows across the full system.
They are slow, brittle, and expensive to maintain.
They catch issues that no other test type can -- but they should be few.

[test-pyramid]: https://martinfowler.com/bliki/TestPyramid.html

### What Contract Tests Do Not Catch

Contract tests are precise in scope.
They verify the contract, nothing more.
They do not catch:

- **Business logic bugs.** If the server computes a wrong price but returns it in the correct format, the contract test passes.
- **Performance issues.** A contract test does not measure response time or throughput.
- **Authentication and authorization flows.** These involve session state, tokens, and multi-step interactions beyond the scope of a single contract.
- **Data consistency.** Whether the data is correct across services is an integration concern.

Contract tests reduce the need for expensive cross-service end-to-end tests.
They catch contract drift early -- in the build, with the speed of unit tests.
They make the overall test suite faster and more reliable by catching the most common integration failure -- structural mismatch -- at the cheapest possible level.

---

## Three Approaches to Contract Testing

Contract testing is not a single technique.
Three distinct approaches exist, defined by **who owns the contract** and **how it is produced**.
Each fits different team structures and development workflows.

### Consumer-Driven

The consumer team writes tests that express its expectations: "I send this request, I expect this response shape."
These expectations are captured in a contract file.
The provider team retrieves the contract and runs it against their implementation to verify compatibility.

The contract reflects **what the consumer actually uses**, not the full API surface.
This is valuable when multiple consumers have different needs and the provider wants to know exactly what each consumer depends on.

The workflow is **sequential**: the consumer must publish its expectations before the provider can verify them.
This requires coordination infrastructure -- a broker or shared repository -- to exchange contracts between teams.

The contract format is typically **proprietary** to the tooling, not an industry standard like OpenAPI.
This means the contracts are not reusable outside the specific tool ecosystem.

### Provider-Driven

The provider team owns the API definition and its lifecycle.
The provider writes the contract, tests it against their implementation, and publishes stubs or mocks for consumers to develop against.

The workflow is again **sequential**, but in the opposite direction.
The provider defines first, and consumers adapt to what the provider publishes.

In practice, provider-driven contracts are often generated from code annotations.
This creates a reliability challenge: annotations describe **intent**, not behavior.
Over time, they drift from the actual implementation -- like documentation that is no longer maintained.
The contract becomes unreliable because it may not represent what the server actually does.

This drift has tangible consequences.
A developer consulting the auto-generated API documentation sees field names, types, and constraints that look authoritative.
When they send a request based on that documentation, it fails because the annotations no longer match the implementation.
The documentation becomes misleading rather than helpful.

### Specification-Driven (Contract-First)

The specification exists **independently** of both consumer and provider.
It is a shared artifact -- an industry-standard document like an OpenAPI specification -- that defines the API contract.
Both sides develop against it.
Neither side owns it exclusively.

The workflow is **parallel**: the provider implements the API to match the specification, and the consumer builds a client against it.
Neither blocks the other.
Contract tests then verify that both sides conform.

This is the approach Contracteer takes.

---

## The Specification as Source of Truth

In specification-driven contract testing, the OpenAPI specification is not a byproduct of implementation.
It is the **starting point**.
Teams agree on the API design -- endpoints, request shapes, response structures, error formats -- before writing code.

This changes the development dynamic in three ways.

**Parallel development.**
The provider team and the consumer team start working at the same time.
The provider implements the server to match the specification.
The consumer builds against a mock server that enforces the specification.
No team waits for the other.

**A shared communication medium.**
The specification becomes the place where teams discuss API design.
Merge requests to the specification become the vehicle for proposing and reviewing changes.
Any team involved with the API can propose changes, regardless of who implements the provider.

**Continuous conformance.**
Contract tests run in the build on every commit.
On the provider side, they verify that the server still responds according to the specification.
On the consumer side, they verify that the client sends valid requests.
Drift is caught immediately, not in staging, not in production.

When a contract test fails, the team faces a clear decision: fix the implementation to match the specification, or update the specification to reflect the intended change.
Either way, the contract stays aligned with reality.

The specification is also **portable**.
It uses an industry standard -- OpenAPI -- that integrates with documentation tools, API gateways, and testing tools.
The contract is not locked into a proprietary format.

---

## Key Takeaways

- Contract testing verifies the boundary between services -- the status codes, response shapes, and headers that both sides depend on.
- Contract tests run fast (no deployment, no infrastructure) and catch the most common integration failure: structural mismatch between what the server sends and what the client expects.
- Three approaches exist: consumer-driven (consumer owns the contract), provider-driven (provider owns the contract), and specification-driven (a shared specification is the source of truth).
- Specification-driven contract testing enables parallel development, uses an industry-standard format, and makes the specification a communication medium between teams.
- Contract tests do not replace other test types, but they significantly reduce the number of integration and end-to-end tests needed by catching structural mismatches early.

---

## Further Reading

- Martin Fowler, [ContractTest](https://martinfowler.com/bliki/ContractTest.html) -- the original definition: what contract tests verify, how they relate to test doubles, and why they check format rather than data values.
- Martin Fowler, [IntegrationTest](https://martinfowler.com/bliki/IntegrationTest.html) -- the narrow vs broad distinction, and how contract tests relate to each kind.
- Martin Fowler, [TestPyramid](https://martinfowler.com/bliki/TestPyramid.html) -- the pyramid model and the rationale for many fast tests at the base, few slow tests at the top.
- Ham Vocke, [The Practical Test Pyramid](https://martinfowler.com/articles/practical-test-pyramid.html) -- a comprehensive guide to each layer of the pyramid, including consumer-driven contract tests in microservice architectures.

---

## Next Steps

- [How Contracteer Works](how-contracteer-works.md) -- how Contracteer turns an OpenAPI specification into a verifier and a mock server.
- [Creating Scenarios](scenarios.md) -- how to write OpenAPI examples that produce the scenarios you want.
- [Testing Your Server](testing-your-server.md) -- how the verifier tests your server against the specification.
- [Testing Your Client](testing-your-client.md) -- how the mock server validates requests and generates responses.
- [Getting Started](../getting-started/index.md) -- set up Contracteer in your project.
