# Troubleshooting

Common issues and how to resolve them.

---

## First Step: Enable HTTP Traffic Logging

When something does not behave as expected, enable HTTP traffic logging.
This shows every request and response exchanged between Contracteer and the server.

For JVM projects, set the `tech.sabai.contracteer.http` logger to DEBUG:

```yaml
# application.yaml (Spring Boot)
logging:
  level:
    tech.sabai.contracteer.http: DEBUG
```

For the CLI, add the `-t` flag:

```bash
contracteer verify openapi.yaml -t
contracteer mock openapi.yaml -t
```

Contracteer also logs automatically at WARN level when a verification case fails or the mock server returns a 418.

---

## Verifier Issues

### All verification cases fail with the wrong status code

**Symptom:** Every scenario-based case expects `200` but the server returns `404`.

**Cause:** The test data is not seeded, or the seeded IDs do not match the OpenAPI example values.

**Fix:** Ensure the seeded data matches the request examples.
If the `ATHOS` scenario sends `GET /musketeers/1`, a musketeer with id `1` must exist in the database.
See [Prepare Test Data](../getting-started/verifier-junit.md#prepare-test-data) for the full pattern.

### Type-mismatch cases fail with 500 instead of 400

**Symptom:** Automatic type-mismatch verification cases expect `400` but the server returns `500`.

**Cause:** The server does not validate input types, or it throws an unhandled exception instead of returning a proper `400` response.

**Fix:** Add input validation to your server.
For Spring Boot, ensure type-mismatch exceptions are mapped to `400` responses.
The response must use the content type and schema declared in your OpenAPI specification (e.g., `application/problem+json` with a ProblemDetail body).

### Response body validation fails

**Symptom:** The verification case reports errors like `'name': expected type 'string' but got 'integer'` or `'email': required field missing`.

**Cause:** The server's response does not conform to the schema declared in the OpenAPI specification.

**Fix:** Compare the server's actual response (visible in DEBUG logs) against the schema.
Common causes:

- A field has the wrong type (e.g., returning a number as a string).
- A required field is missing from the response.
- The `Content-Type` header does not match the declared media type.

### "Additional properties are not allowed"

**Symptom:** The verifier rejects a response or the mock server rejects a request with "Additional properties are not allowed. Unexpected properties: ..."

**Cause:** The schema declares `additionalProperties: false` and the body contains a field not listed in the schema's `properties`.

**Fix:** Contracteer enforces `additionalProperties: false` as a declared constraint, even though the default behavior is tolerant of extra fields.
Either add the field to the schema's `properties`, or remove `additionalProperties: false` if the constraint is not intentional.

### No scenarios created for an operation

**Symptom:** An operation has examples in the specification, but Contracteer does not generate scenario-based verification cases for it.

**Cause:** The example keys exist only on the request side or only on the response side.
Contracteer needs at least one shared key between request and response elements.

**Fix:** Ensure the same example key appears on both a request element (parameter or request body) and a response element (header or response body).
See [Common Mistakes](../concepts/scenarios.md#common-mistakes) for details.

---

## Mock Server Issues

### Getting 418 instead of the expected response

**Symptom:** The mock server returns `418` instead of the response you expected.

**Cause:** The 418 is Contracteer's diagnostic signal.
It means the mock server received the request but cannot determine the correct response.

**Fix:** Read the 418 response body -- it explains what went wrong.
Common causes:

- **No scenario matches.** The request values do not exactly match any scenario's example values.
  Check that your request sends the exact values from the OpenAPI examples.
- **Multiple scenarios match.** The request matches more than one scenario.
  Make your example values more specific to distinguish them.
- **Multiple 2xx response codes.** The operation defines more than one success status code and no scenario disambiguates.
  Add scenarios to target specific status codes.

See [The 418 Diagnostic Response](../concepts/testing-your-client.md#the-418-diagnostic-response) for a full explanation.

### Getting 400 when expecting 200

**Symptom:** The mock server returns `400` for a request you believe is valid.

**Cause:** The request violates the OpenAPI schema.
The mock server validates every request against the full schema -- including types, required fields, enum constraints, and string formats.

**Fix:** Compare your request against the schema.
Common causes:

- A required field is missing from the request body.
- A field value is not in the declared enum.
- A parameter has the wrong type (e.g., a string where an integer is expected).
- The `Content-Type` header does not match the declared media type.

Enable DEBUG logging to see the exact request the mock server received.

### Request rejected for sending a readOnly field

**Symptom:** The mock server rejects a request with "Additional properties are not allowed" for a field like `id`.

**Cause:** The field is marked `readOnly: true` in the schema and the schema has `additionalProperties: false`.
Contracteer excludes `readOnly` properties from the request schema.
If the client sends a `readOnly` field, it is treated as an unexpected additional property.

**Fix:** Remove the `readOnly` field from your request.
`readOnly` properties like `id` are server-generated -- they belong in responses, not in requests.

### Verifier fails because a writeOnly field is missing from the response

**Symptom:** The verifier reports a missing required field for a property like `password`.

**Cause:** The field is marked `writeOnly: true` in the schema.
Contracteer excludes `writeOnly` properties from the response schema.
If the field is also `required`, the server should not return it and the verifier should not expect it.

**Fix:** This is usually correct behavior.
If the verifier fails, check that the field is actually marked `writeOnly: true` in your specification.
If it is, the server should not include it in responses.

### Response values are random and different on each run

**Symptom:** The mock server returns valid responses, but the values change on every request.

**Cause:** The request does not match any scenario.
When no scenario matches, the mock server generates a response from the schema with random values.

**Fix:** This is expected behavior when no scenario is defined for the request.
If you need deterministic responses, define scenarios in your OpenAPI specification.
Use `examples` on both request and response elements.
See [Creating Scenarios](../concepts/scenarios.md) for how to do this.

---

## Specification Issues

### Spec loading fails

**Symptom:** Contracteer reports errors when loading the OpenAPI specification.

**Common causes:**

- **Invalid YAML or JSON.** Check syntax with a YAML linter.
- **Unsupported OpenAPI version.** Contracteer supports OpenAPI 3.0.x only.
  OpenAPI 3.1 is not yet supported.
- **`example` and `examples` on the same element.** The OpenAPI specification declares these mutually exclusive.
  Contracteer rejects the specification if both are present on the same parameter or media type.
- **Missing 2xx response.** Contracteer requires every operation to define at least one 2xx response.

### No scenarios created at all

**Symptom:** The specification has example values, but Contracteer creates no scenarios.

**Cause:** The examples are defined under `schema.properties` instead of on the parameter or media type.
Contracteer reads examples from parameters and media types, not from schema properties.

```yaml
# This does NOT produce a scenario
schema:
  type: object
  properties:
    name:
      type: string
      example: Athos    # not used by Contracteer
```

**Fix:** Move examples to the parameter or media type level using the `examples` or `example` keyword.
See [Creating Scenarios](../concepts/scenarios.md) for the correct placement.

### Unexpected behavior from unsupported schema keywords

**Symptom:** Verification fails or the mock server rejects valid requests / returns wrong responses, even though the specification looks correct.

**Cause:** Your specification uses an OpenAPI schema keyword that Contracteer does not yet support.
The keyword is silently ignored, which changes validation behavior.

Common examples:

- **`pattern`** on a string field (e.g., `"^\d{5}$"` for a zip code).
  The verifier sends random strings that don't match the pattern, causing the real server to reject them.
- **`minItems`** on an array.
  The verifier may send an empty array when the server expects at least one item.

**Fix:** Check the [OpenAPI 3.0 Coverage](../concepts/openapi-coverage.md) page for the full list of supported and unsupported keywords.
If your specification relies on an unsupported keyword, you may need to work around it until support is added.

### Operations missing from verification

**Symptom:** Some operations in your specification are not tested.
Contracteer produces no verification cases for them.

**Cause:** The operation uses a feature that Contracteer does not yet support.
Operations are skipped when they use:

- `multipart/form-data`, `application/x-www-form-urlencoded`, or `application/xml` content types.
- Array or object parameters (path, query, header, cookie).

Contracteer logs a warning for each skipped operation.

**Fix:** Check the [OpenAPI 3.0 Coverage](../concepts/openapi-coverage.md) page for the full list of supported features.

### Confusing `example` and `examples`

`example` provides a single value.
`examples` is a named map where each entry has a `value` field:

```yaml
# Single value
example: 42

# Named map -- each name is an example key
examples:
  ATHOS:
    value: 42
  PORTHOS:
    value: 43
```

Both are valid.
Contracteer uses both for scenario creation.
See [OpenAPI Examples](../concepts/scenarios.md#openapi-examples-examples-and-example) for details.

---

## Next Steps

- [OpenAPI 3.0 Coverage](../concepts/openapi-coverage.md) -- which OpenAPI features are supported and which are not.
- [Creating Scenarios](../concepts/scenarios.md) -- how to write OpenAPI examples that produce the scenarios you want.
- [Testing Your Server](../concepts/testing-your-server.md) -- what the verifier checks in depth.
- [Testing Your Client](../concepts/testing-your-client.md) -- how the mock server validates requests and generates responses.
