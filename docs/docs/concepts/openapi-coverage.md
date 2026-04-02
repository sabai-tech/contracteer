# OpenAPI 3.0 Coverage

This page describes which parts of the OpenAPI 3.0 specification Contracteer supports, where it deviates, and which
features are not applicable to contract testing.

Contracteer supports OpenAPI 3.0.x.

---

## Limitations

Contracteer does not reject a specification because it contains unsupported features.
Unsupported content types cause the operation to be skipped with a warning.
Unsupported schema keywords are ignored.
Your specification still loads.

| Feature                           | Impact                                                                                    |
|-----------------------------------|-------------------------------------------------------------------------------------------|
| `application/xml`                 | Operations with XML-only content types are skipped with a warning.                        |
| `not` (schema negation)           | The keyword is ignored. Values are validated and generated without it.                    |
| `allowEmptyValue` (parameters)    | The keyword is ignored. Deprecated by the OAS 3.0 specification itself.                   |
| `externalValue` (Example Objects) | Only inline `value` is read. External references are not fetched.                         |
| Pattern generation (regex)        | Some regex features (lookaheads, lookbehinds) do not generate correctly.                  |
| Unknown integer/number formats    | Ignored with a warning. Only `int32`, `int64`, `float`, `double` apply range constraints. |

If Contracteer skips an operation or ignores a keyword, it logs a warning when loading the specification.

---

## Deviations from the Specification

The OpenAPI 3.0 specification is sometimes ambiguous or impractical for contract testing.
In these cases, Contracteer makes explicit choices.

### String constraint precedence

OpenAPI treats `format`, `pattern`, `minLength`, and `maxLength` as independent constraints applied simultaneously.
Contracteer applies them in a strict precedence order:

1. **`format`** (email, uuid, date, date-time, byte, binary) -- the format defines the type entirely.
   `pattern` is always ignored with a warning.
   `minLength`/`maxLength` are supported by email, base64, and binary formats, but ignored by uuid, date, and date-time.
2. **`pattern`** -- the regex pattern defines the constraint.
   `minLength` and `maxLength` are ignored with a warning.
3. **`minLength` / `maxLength`** -- used only when neither `format` nor `pattern` is set.

Generating a random value that satisfies both a regex pattern and a length range is not reliably possible.
Contracteer uses the same constraint for both validation and generation to ensure consistency.
What Contracteer validates is what it can generate.

`enum` is always validated against the active constraint when loading the specification.
If all enum values satisfy the active constraint, the specification is accepted.
If any enum value violates it, the specification is rejected.

Random value generation for patterns uses the [RgxGen](https://github.com/curious-odd-man/rgxgen) library.
RgxGen supports most common regex features, but lookaheads and lookbehinds do not generate correctly.
If Contracteer generates invalid values for your pattern, use `enum` values instead, or provide explicit `examples` in
your specification.

### Multipart default content types

The OAS 3.0 specification defaults arrays of primitives to `text/plain` in multipart parts.
Contracteer uses `application/json` for all arrays.
The specification does not define how to serialize an array as plain text in a multipart part.

| Property type                                       | Contracteer default        |
|-----------------------------------------------------|----------------------------|
| Primitive                                           | `text/plain`               |
| Array of primitives                                 | `application/json`         |
| Array of `format: binary` or `format: base64` items | `application/octet-stream` |
| Object or composite                                 | `application/json`         |
| `format: binary` or `format: base64`                | `application/octet-stream` |

Use the `encoding` object to override the default content type for specific properties.

### `allOf` with non-structured sub-schemas

A common real-world pattern wraps a `$ref` in a single-element `allOf` for documentation or tooling reasons:

```yaml
components:
  schemas:
    NonEmptyString:
      type: string
      minLength: 1
    ProductName:
      allOf:
        - $ref: '#/components/schemas/NonEmptyString'
```

Contracteer handles single-element `allOf` with any sub-schema type -- string, integer, object, or composite.

**Multi-element `allOf`** requires all sub-schemas to be structured types (object, `allOf`, `anyOf`, or `oneOf`):

```yaml
# Supported: multi-element allOf with objects
allOf:
  - $ref: '#/components/schemas/Pet'
  - type: object
    properties:
      huntingSkill:
        type: string

# Not supported: multi-element allOf with primitives
allOf:
  - type: string
    minLength: 5
  - type: string
    maxLength: 10
```

The OAS 3.0 specification allows `allOf` sub-schemas of any type, but Contracteer generates random values for each
sub-schema and merges the results.
For objects, this works -- different sub-schemas contribute different properties that combine cleanly.
For primitives, constraints overlap on the same value.
A `minLength: 5` from one sub-schema and a `maxLength: 10` from another would require computing the constraint
intersection, which Contracteer does not do.

If your specification uses multi-element `allOf` with primitives, combine the constraints into a single schema:

```yaml
# Equivalent single schema
type: string
minLength: 5
maxLength: 10
```

### Multiple composition keywords on the same schema

JSON Schema allows `allOf`, `anyOf`, and `oneOf` to appear on the same schema, with each keyword applying independently:

```yaml
# Valid per the spec, but rejected by Contracteer
ProductOrService:
  allOf:
    - $ref: '#/components/schemas/Purchasable'
  oneOf:
    - $ref: '#/components/schemas/Product'
    - $ref: '#/components/schemas/Service'
```

Contracteer rejects schemas that combine multiple composition keywords.
Each composition keyword produces a different validation strategy (all must match, exactly one must match, at least one
must match).
Combining them would require a compound validation that Contracteer does not implement.

This pattern is extremely rare in real-world specifications and is almost always a mistake.
If your specification combines composition keywords, restructure it to use a single keyword:

```yaml
# Use allOf to combine the base schema with a oneOf
ProductOrService:
  allOf:
    - $ref: '#/components/schemas/Purchasable'
    - oneOf:
        - $ref: '#/components/schemas/Product'
        - $ref: '#/components/schemas/Service'
```

### `nullable` on composition schemas

The OAS 3.0.4 specification states that `nullable` only takes effect when `type` is explicitly defined on the same
schema.
Composition schemas (`oneOf`, `anyOf`, `allOf`) typically do not declare `type`, which means `nullable: true` should
technically have no effect:

```yaml
MySchema:
  nullable: true
  oneOf:
    - $ref: '#/components/schemas/Cat'
    - $ref: '#/components/schemas/Dog'
```

Contracteer honours `nullable: true` on composition schemas regardless of whether `type` is present.
This matches the behavior of most OpenAPI tools (swagger-codegen, OpenAPI Generator, Redoc) and what users expect.
The OAS 3.0 `nullable` rule is widely considered a specification design flaw, which OAS 3.1 resolved by replacing
`nullable` with `type` arrays (e.g., `type: ["object", "null"]`).

---

## Not Applicable to Contract Testing

These features are defined in the OAS 3.0 specification but do not affect the structural contract between client and
server.
Contracteer does not process them.

| Feature                          | Reason                                                                                                                                 |
|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| `servers` / server variables     | Connection configuration. The verifier accepts the server URL as external configuration.                                               |
| `security` / `securitySchemes`   | Authentication is runtime configuration, not contract structure.                                                                       |
| `tags`, `summary`, `description` | Documentation metadata.                                                                                                                |
| `operationId`                    | Client code generation hint.                                                                                                           |
| `externalDocs`                   | Documentation link.                                                                                                                    |
| `deprecated`                     | Informational flag. A deprecated operation has the same structural contract.                                                           |
| `callbacks`                      | Webhook definitions -- a separate concern from request/response contract testing.                                                      |
| `links`                          | Hypermedia navigation hints between operations.                                                                                        |
| `default` (property values)      | The verifier always sends values. The mock server validates against the schema, not against assumed defaults.                          |
| `example` on Schema Object       | Contracteer uses the `examples` keyword on Parameter and Media Type Objects for scenario creation. Schema-level `example` is not read. |
| `xml` (Schema Object)            | XML serialization hints. Only relevant if XML content type were supported.                                                             |

---

## Detailed Coverage

### Data types

| Type      | Notes                                                                                                                                           |
|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| `string`  | With `minLength`, `maxLength`, `pattern`, `enum`, `nullable`                                                                                    |
| `integer` | With `minimum`, `maximum`, `exclusiveMinimum`, `exclusiveMaximum`, `multipleOf`, `enum`, `nullable`                                             |
| `number`  | With `minimum`, `maximum`, `exclusiveMinimum`, `exclusiveMaximum`, `multipleOf`, `enum`, `nullable`                                             |
| `boolean` | With `enum`, `nullable`                                                                                                                         |
| `array`   | With `minItems`, `maxItems`, `uniqueItems`. Including array parameters with style/explode encoding                                              |
| `object`  | With `minProperties`, `maxProperties`, `additionalProperties`, `readOnly`, `writeOnly`. Including object parameters with style/explode encoding |

### String formats

| Format          | Status                              |
|-----------------|-------------------------------------|
| `date`          | Supported (ISO 8601 `YYYY-MM-DD`)   |
| `date-time`     | Supported (ISO 8601 with offset)    |
| `email`         | Supported                           |
| `uuid`          | Supported                           |
| `byte` (base64) | Supported                           |
| `binary`        | Supported                           |
| `password`      | Supported (treated as plain string) |
| Custom formats  | Ignored                             |

### Integer and number formats

| Format   | Status                                                                         |
|----------|--------------------------------------------------------------------------------|
| `int32`  | Supported. Applies 32-bit signed range; rejects explicit min/max outside range |
| `int64`  | Supported. Applies 64-bit signed range; rejects explicit min/max outside range |
| `float`  | Supported. Applies 32-bit float range; rejects explicit min/max outside range  |
| `double` | Supported. Applies 64-bit double range; rejects explicit min/max outside range |
| Other    | Ignored with a warning. No range constraint applied                            |

### Schema keywords

| Keyword                                 | Notes                                                                                 |
|-----------------------------------------|---------------------------------------------------------------------------------------|
| `nullable`                              | All data types                                                                        |
| `enum`                                  | All data types                                                                        |
| `minimum` / `maximum`                   | Integer and number types                                                              |
| `exclusiveMinimum` / `exclusiveMaximum` | Integer and number types (OAS 3.0 boolean semantics)                                  |
| `minLength` / `maxLength`               | String, email, base64, binary types                                                   |
| `pattern`                               | Plain strings only. See [string constraint precedence](#string-constraint-precedence) |
| `multipleOf`                            | Integer and number types                                                              |
| `minItems` / `maxItems`                 | Array types                                                                           |
| `uniqueItems`                           | Array types                                                                           |
| `minProperties` / `maxProperties`       | Object types                                                                          |
| `required` (properties)                 | Object types                                                                          |
| `additionalProperties`                  | Both boolean and typed schema forms                                                   |
| `readOnly` / `writeOnly`                | readOnly properties excluded from request schemas, writeOnly from response schemas    |
| `discriminator`                         | With `propertyName` and `mapping` on allOf, oneOf, anyOf                              |

### Schema composition

| Feature         | Notes                                                                                                                                                                                     |
|-----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `allOf`         | Single-element accepts any sub-schema type; multi-element requires structured types. Sibling `properties`, `required`, and `additionalProperties` are folded in as an implicit sub-schema |
| `oneOf`         | Validates that exactly one sub-schema matches. Sibling `properties`, `required`, and `additionalProperties` are supported                                                                 |
| `anyOf`         | Validates that at least one sub-schema matches. Sibling `properties`, `required`, and `additionalProperties` are supported                                                                |
| `discriminator` | Supported on allOf, oneOf, anyOf with `propertyName` and `mapping`                                                                                                                        |

### Parameters

| Feature                         | Notes                                                                                               |
|---------------------------------|-----------------------------------------------------------------------------------------------------|
| `in: path`                      | Primitive, array, and object types. Styles: `simple`, `label`, `matrix`                             |
| `in: query`                     | Primitive, array, and object types. Styles: `form`, `spaceDelimited`, `pipeDelimited`, `deepObject` |
| `in: header`                    | Primitive, array, and object types. Style: `simple`                                                 |
| `in: cookie`                    | Primitive, array, and object types. Style: `form`                                                   |
| `style` / `explode`             | All OAS 3.0 style/explode combinations with correct defaults per location                           |
| `content` (instead of `schema`) | Parameter value serialized via content type (e.g., JSON-encoded query parameter)                    |
| `allowReserved`                 | Query parameters and `application/x-www-form-urlencoded` encoding properties                        |

### Request and response bodies

| Feature                                | Notes                                                       |
|----------------------------------------|-------------------------------------------------------------|
| `application/json`                     | Supported                                                   |
| Plain text content types               | Supported                                                   |
| `multipart/*` (form-data, mixed, etc.) | Per-part content type via the `encoding` object             |
| `application/x-www-form-urlencoded`    | Per-property encoding via the `encoding` object             |
| Multiple content types                 | Produces one verification per content type combination      |
| `required` (request body)              | Supported                                                   |
| Content negotiation (Accept header)    | RFC 7231 support with quality factors and wildcard subtypes |

### Examples and scenarios

| Feature                                         | Notes                                            |
|-------------------------------------------------|--------------------------------------------------|
| `examples` map (parameter / media type level)   | Core mechanism for scenario creation             |
| Single `example` (parameter / media type level) | Supported                                        |
| Status-code-prefixed example keys               | Supported (`404_NOT_FOUND`, `400_INVALID_INPUT`) |

See [Creating Scenarios](scenarios.md) for how examples drive scenario creation.

### References ($ref)

| Feature                                | Notes                                                                 |
|----------------------------------------|-----------------------------------------------------------------------|
| `$ref` to `#/components/schemas`       | Supported                                                             |
| `$ref` to `#/components/parameters`    | Supported                                                             |
| `$ref` to `#/components/requestBodies` | Supported                                                             |
| `$ref` to `#/components/responses`     | Supported                                                             |
| `$ref` to `#/components/headers`       | Supported                                                             |
| `$ref` to `#/components/examples`      | Supported                                                             |
| Recursive / chained `$ref`             | Supported (with depth limits)                                         |
| External `$ref` (other files)          | Resolved by the OpenAPI parser before Contracteer processes the model |

### Responses

| Feature                                  | Notes                                                                                 |
|------------------------------------------|---------------------------------------------------------------------------------------|
| Exact status codes (`200`, `404`, etc.)  | Supported                                                                             |
| Status code ranges (`2XX`, `4XX`, `5XX`) | Used as fallback when exact status code is not defined                                |
| `default` response                       | Used as fallback when neither an exact status code nor a status code range is defined |
| Response headers                         | Supported                                                                             |
| Response body                            | Supported                                                                             |

---

## Next Steps

- [Troubleshooting](../guides/troubleshooting.md) -- common issues caused by unsupported features and how to work around
  them.
- [Creating Scenarios](scenarios.md) -- how OpenAPI examples become scenarios and verification cases.
- [Testing Your Server](testing-your-server.md) -- what the verifier checks.
- [Testing Your Client](testing-your-client.md) -- how the mock server validates requests and generates responses.