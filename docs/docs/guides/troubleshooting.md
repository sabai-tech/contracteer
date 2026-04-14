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

### Verifier reports "Ambiguous match for oneOf"

**Symptom:** The verifier rejects a response with "Ambiguous match for 'oneOf'. The provided value matches multiple schemas."

**Cause:** The `oneOf` variants have overlapping structures.
Without `required` properties, `additionalProperties: false`, or a `discriminator`, a valid value for one variant inevitably matches others too.

**Fix:** Make the variants distinguishable.
Add `required` properties unique to each variant, set `additionalProperties: false`, or use a `discriminator`.
Without these constraints, `oneOf` is effectively `anyOf` and strict validation cannot determine which variant matched.

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

### Extraction fails with minProperties/maxProperties and readOnly/writeOnly

**Symptom:** Loading the specification fails with an error about `minProperties`/`maxProperties` combined with `readOnly` or `writeOnly` properties.

**Cause:** The OAS specification does not define how `minProperties`/`maxProperties` interact with `readOnly`/`writeOnly`.
When properties are excluded from request or response schemas, the property count constraints become ambiguous.
Contracteer rejects this combination to avoid silent misinterpretation.

**Fix:** Remove either the `minProperties`/`maxProperties` constraint or the `readOnly`/`writeOnly` annotations from the affected object schema.

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

### Circular schema reference rejected

**Symptom:** Loading the specification fails with "Circular reference with no optional, nullable, or collection exit point."

**Cause:** The specification contains a circular `$ref` chain where every property in the cycle is required and non-nullable.
This describes an infinite structure that cannot be instantiated.

For example, `Node.next → Link.target → Node` where both `next` and `target` are required and non-nullable creates a structure with no valid finite value.

**Fix:** Break the cycle by making at least one property in the chain optional (remove it from `required`), nullable (`nullable: true` on the referenced schema), or a collection (`type: array`).
Any of these gives Contracteer a finite stopping point.

### Recursive array generates fewer items than minItems

**Symptom:** The verifier reports "Array has 0 items but minItems is N" on a recursive schema property.

**Cause:** The schema contains a recursive `$ref` cycle through an array property with a `minItems` constraint.
At the recursion depth limit, Contracteer generates an empty array to avoid producing null values for non-nullable items.
This may violate the `minItems` constraint.

**Fix:** Remove the `minItems` constraint from the recursive array property, or make the array items nullable.
Recursive arrays with `minItems` describe a structure that requires infinite depth to satisfy -- no finite value can conform.

### Spec loading fails

**Symptom:** Contracteer reports errors when loading the OpenAPI specification.

**Common causes:**

- **Invalid YAML or JSON.** Check syntax with a YAML linter.
- **Unsupported OpenAPI version.** Contracteer supports OpenAPI 3.0.x only.
  OpenAPI 3.1 is not yet supported.
- **`example` and `examples` on the same element.** The OpenAPI specification declares these mutually exclusive.
  Contracteer rejects the specification if both are present on the same parameter or media type.
- **Multiple composition keywords on the same schema.** A schema combining `allOf`, `anyOf`, or `oneOf` at the same level is rejected.
  Restructure the schema to use a single composition keyword.
  See [Multiple composition keywords](../concepts/openapi-coverage.md#multiple-composition-keywords-on-the-same-schema) for the recommended pattern.

### Extraction fails with nested types in deepObject or form-urlencoded

**Symptom:** Loading the specification fails with "does not support nested objects or arrays in properties (undefined behavior in the OpenAPI specification)."

**Cause:** A `deepObject` query parameter or a `form-urlencoded` request body has properties with object or array types.
The OpenAPI specification explicitly states that the behavior of `deepObject` for nested objects and arrays is undefined.
Similarly, `form` style serialization is only defined for primitive property values.

**Fix:** Restructure the schema to use only primitive properties, or switch to a JSON-encoded parameter using the `content` keyword instead of `style`/`explode`.

### Extraction fails with non-JSON content type and structured schema

**Symptom:** Loading the specification fails with "Content type [text/plain|text/html|application/jwt] supports only primitive schemas."

**Cause:** A request or response body uses a media type that has no standard serialization for structured values -- `text/plain`, `text/html`, and `application/jwt` all describe scalar textual payloads.
The OpenAPI specification does not define how to serialize an object, array, or composition for these media types, so any implementation would rely on an implicit convention between the spec author and the client.
Contracteer rejects the combination at load time rather than silently applying a guess.

**Fix:** Change the content type to `application/json` if the schema describes a structured value, or simplify the schema to a primitive type if the content type must remain as declared.

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

### Warning about ignored pattern or length constraints

**Symptom:** Contracteer logs a warning like "pattern ignored because format takes precedence" or "minLength/maxLength ignored because pattern takes precedence."

**Cause:** Your schema combines constraints that Contracteer applies in precedence order: `format` > `pattern` > `minLength`/`maxLength`.
The lower-priority constraint is ignored for both validation and generation.

**Fix:** This is intentional.
See [String constraint precedence](../concepts/openapi-coverage.md#string-constraint-precedence) for the full explanation.
If you need the pattern to apply, remove the `format`.
If you need length constraints to apply, remove the `pattern`.

### Pattern not supported for value generation

**Symptom:** Loading the specification fails with "pattern is not supported for value generation."

**Cause:** The `pattern` is a valid regex but Contracteer cannot generate values that match it.
This happens when the pattern uses constructs the value generator cannot handle, such as:

- Lookahead or lookbehind assertions combined with anchors (e.g., `^(?!foo:)[a-zA-Z]+$`)
- Anchors inside alternation branches (e.g., `(?:^foo$)|(?:^bar$)`)
- Deeply nested quantifiers that overflow the regex engine (e.g., `[a-z]{0,63}:[a-z]{0,63}:.{0,1023}`)
- Inline flag modifiers (e.g., `(?i)foo`)

Contracteer automatically rewrites common Java-specific constructs (POSIX classes like `\p{Print}`, Java aliases like `\p{IsLetter}`, dash-position issues in character classes) into compatible equivalents.
Patterns that cannot be rewritten are rejected at load time.

**Fix:** Provide OpenAPI `examples` on the parameter or media type to create Scenarios with explicit values for this property.
Contracteer uses Scenario values instead of generating random ones, bypassing the pattern limitation entirely.
See [Creating Scenarios](../concepts/scenarios.md) for how to provide examples.
Alternatively, simplify the pattern to use standard character classes and quantifiers without lookaround assertions.

### Unexpected behavior from unsupported schema keywords

**Symptom:** Verification fails or the mock server rejects valid requests / returns wrong responses, even though the specification looks correct.

**Cause:** Your specification uses an OpenAPI schema keyword that Contracteer does not yet support.
The keyword is silently ignored, which changes validation behavior.

Common examples:

- **`default`** on a property.
  The verifier may omit an optional property when the server expects the default value.

**Fix:** Check the [OpenAPI 3.0 Coverage](../concepts/openapi-coverage.md) page for the full list of supported and unsupported keywords.
If your specification relies on an unsupported keyword, you may need to work around it until support is added.

### Operations missing from verification

**Symptom:** Some operations in your specification are not tested.
Contracteer produces no verification cases for them.

**Cause:** The operation uses a feature that Contracteer does not yet support.
Operations are skipped when they use:

- `application/xml` content types.
- Request or response bodies declared without a schema (e.g., `application/json: {}`).
- Parameters using the `content` keyword without a schema.

Contracteer logs a warning for each skipped operation.

**Fix:** Add a schema to the content type declaration, or remove the content type if no schema is needed.
Check the [OpenAPI 3.0 Coverage](../concepts/openapi-coverage.md) page for the full list of supported features.

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
