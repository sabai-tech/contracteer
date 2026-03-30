# OpenAPI 3.0 Coverage

Contracteer supports OpenAPI 3.0.x.
OpenAPI 3.1 is not yet supported.

This page lists which features of the specification are supported and which are not yet supported.
It also explains what happens when your specification uses an unsupported feature.

---

## What Happens with Unsupported Features

Contracteer does not reject a specification because it contains unsupported features.
Instead, it handles them gracefully:

- **Unsupported content types** (XML) -- the operation is skipped with a warning.
- **Unsupported schema keywords** (`default`, `not`) -- the keyword is ignored.
  Values are generated and validated without that constraint.

Your specification still loads.
Only the affected operations or constraints are skipped.

---

## Data Types

| Type      | Status    | Notes                                                                                 |
|-----------|-----------|---------------------------------------------------------------------------------------|
| `string`  | Supported | With `minLength`, `maxLength`, `enum`, `nullable`                                     |
| `integer` | Supported | With `minimum`, `maximum`, `exclusiveMinimum`, `exclusiveMaximum`, `enum`, `nullable` |
| `number`  | Supported | With `minimum`, `maximum`, `exclusiveMinimum`, `exclusiveMaximum`, `enum`, `nullable` |
| `boolean` | Supported | With `enum`, `nullable`                                                               |
| `array`   | Supported | Including array parameters with style/explode encoding                                |
| `object`  | Supported | Including object parameters with style/explode encoding                               |

---

## String Formats

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

---

## Integer and Number Formats

| Format          | Status                                                                         |
|-----------------|--------------------------------------------------------------------------------|
| `int32`         | Supported. Applies 32-bit signed range; rejects explicit min/max outside range |
| `int64`         | Supported. Applies 64-bit signed range; rejects explicit min/max outside range |
| `float`         | Supported. Applies 32-bit float range; rejects explicit min/max outside range  |
| `double`        | Supported. Applies 64-bit double range; rejects explicit min/max outside range |
| Unknown formats | Rejected                                                                       |

---

## Schema Keywords

### Supported

| Keyword                                 | Notes                                                                               |
|-----------------------------------------|-------------------------------------------------------------------------------------|
| `nullable`                              | All data types                                                                      |
| `enum`                                  | All data types                                                                      |
| `minimum` / `maximum`                   | Integer and number types                                                            |
| `exclusiveMinimum` / `exclusiveMaximum` | Integer and number types (OAS 3.0 boolean semantics)                                |
| `minLength` / `maxLength`               | String, email, base64, binary types                                                 |
| `required` (properties)                 | Object types                                                                        |
| `additionalProperties`                  | Both boolean and typed schema forms                                                 |
| `discriminator`                         | With `propertyName` and `mapping` on allOf, oneOf, anyOf                            |
| `readOnly` / `writeOnly`                | readOnly properties excluded from request schemas, writeOnly from response schemas  |
| `pattern`                               | Plain strings only. Ignored when `format` is set. Overrides `minLength`/`maxLength` |
| `multipleOf`                            | Integer and number types                                                            |
| `minItems` / `maxItems`                 | Array types                                                                         |
| `uniqueItems`                           | Array types                                                                         |
| `minProperties` / `maxProperties`       | Object types. Cannot be combined with `readOnly` or `writeOnly` properties          |

### Not yet supported

| Keyword                     | Impact                                     |
|-----------------------------|--------------------------------------------|
| `default` (property values) | Default values are not used for generation |
| `not`                       | Schema negation is not supported           |

### String constraint precedence

OpenAPI allows combining `format`, `pattern`, `minLength`, and `maxLength` on the same string schema.
Contracteer applies them in a strict precedence order for both validation and random value generation:

1. **`format`** (email, uuid, date, date-time, byte, binary) -- the format defines the type entirely.
   `pattern` is always ignored with a warning.
   `minLength`/`maxLength` are supported by email, base64, and binary formats, but ignored by uuid, date, and date-time.
2. **`pattern`** -- the regex pattern defines the constraint.
   `minLength` and `maxLength` are ignored with a warning.
3. **`minLength` / `maxLength`** -- used only when neither `format` nor `pattern` is set.

This deviates from the OpenAPI specification, which treats all keywords as independent constraints applied
simultaneously.
Contracteer takes this approach because generating a random value that satisfies both a regex pattern and a length range
is not reliably possible.
Validation and generation use the same constraint to ensure consistency: what Contracteer validates is what it can
generate.

`enum` is always validated against the active constraint at extraction time.
If all enum values satisfy the active constraint, the specification is accepted.
If any enum value violates it, the specification is rejected.

#### Pattern support and limitations

Random value generation for patterns uses the [RgxGen](https://github.com/curious-odd-man/rgxgen) library.
RgxGen supports most common regex features, but some patterns do not generate correctly:

- Lookaheads and lookbehinds are not supported
- Very complex patterns may fail to generate or produce invalid values

If Contracteer generates invalid values for your pattern, use `enum` values instead, or provide explicit `examples` in
your specification.

---

## Schema Composition

| Feature         | Status                                                                                       |
|-----------------|----------------------------------------------------------------------------------------------|
| `allOf`         | Supported. Contracteer requires all sub-schemas to be structured types (object or composite) |
| `oneOf`         | Supported. Validates that exactly one sub-schema matches                                     |
| `anyOf`         | Supported. Validates that at least one sub-schema matches                                    |
| `discriminator` | Supported on allOf, oneOf, anyOf with `propertyName` and `mapping`                           |
| `not`           | Not supported                                                                                |

---

## Parameters

| Feature                         | Status        | Notes                                                                                               |
|---------------------------------|---------------|-----------------------------------------------------------------------------------------------------|
| `in: path`                      | Supported     | Primitive, array, and object types. Styles: `simple`, `label`, `matrix`                             |
| `in: query`                     | Supported     | Primitive, array, and object types. Styles: `form`, `spaceDelimited`, `pipeDelimited`, `deepObject` |
| `in: header`                    | Supported     | Primitive, array, and object types. Style: `simple`                                                 |
| `in: cookie`                    | Supported     | Primitive, array, and object types. Style: `form`                                                   |
| `style` / `explode`             | Supported     | All OAS 3.0 style/explode combinations with correct defaults per location                           |
| `content` (instead of `schema`) | Not supported |                                                                                                     |
| `allowEmptyValue`               | Not supported |                                                                                                     |
| `allowReserved`                 | Supported     | Query parameters and `application/x-www-form-urlencoded` encoding properties                        |

---

## Request and Response Bodies

| Feature                                | Status                                                        |
|----------------------------------------|---------------------------------------------------------------|
| `application/json`                     | Supported                                                     |
| Plain text content types               | Supported                                                     |
| `application/xml`                      | Not supported. Operations using it are skipped                |
| `multipart/*` (form-data, mixed, etc.) | Supported. Per-part content type via the `encoding` object    |
| `application/x-www-form-urlencoded`    | Supported. Per-property encoding via the `encoding` object    |
| Multiple content types                 | Supported. Produces a cartesian product of verification cases |
| `required` (request body)              | Supported                                                     |

### Multipart default content types

For multipart request bodies, Contracteer assigns a default content type to each part based on the property type:

| Property type                        | Default Content-Type       |
|--------------------------------------|----------------------------|
| Primitive                            | `text/plain`               |
| Array (any item type)                | `application/json`         |
| Object or composite                  | `application/json`         |
| `format: binary` or `format: base64` | `application/octet-stream` |

This deviates from the OAS specification, which defaults arrays of primitives to `text/plain`.
Contracteer uses `application/json` for all arrays because the specification does not define how to serialize an array
as plain text in multipart parts.
Use the `encoding` object to override the default content type for specific properties.

---

## Examples and Scenarios

| Feature                                         | Status                                           |
|-------------------------------------------------|--------------------------------------------------|
| `examples` map (parameter / media type level)   | Supported. Core mechanism for scenario creation  |
| Single `example` (parameter / media type level) | Supported                                        |
| Status-code-prefixed example keys               | Supported (`404_NOT_FOUND`, `400_INVALID_INPUT`) |
| `example` on schema properties                  | Not used for scenario creation                   |
| `externalValue` in Example Objects              | Not supported. Only inline `value` is read       |

See [Creating Scenarios](scenarios.md) for how examples drive scenario creation.

---

## References ($ref)

| Feature                                | Status                                                                            |
|----------------------------------------|-----------------------------------------------------------------------------------|
| `$ref` to `#/components/schemas`       | Supported                                                                         |
| `$ref` to `#/components/parameters`    | Supported                                                                         |
| `$ref` to `#/components/requestBodies` | Supported                                                                         |
| `$ref` to `#/components/responses`     | Supported                                                                         |
| `$ref` to `#/components/headers`       | Supported                                                                         |
| `$ref` to `#/components/examples`      | Supported                                                                         |
| Recursive / chained `$ref`             | Supported (with depth limits)                                                     |
| External `$ref` (other files)          | Supported (resolved by the OpenAPI parser before Contracteer processes the model) |

---

## Responses

| Feature                                  | Status                                                                                           |
|------------------------------------------|--------------------------------------------------------------------------------------------------|
| Exact status codes (`200`, `404`, etc.)  | Supported                                                                                        |
| Status code ranges (`2XX`, `4XX`, `5XX`) | Supported. Used as fallback when exact status code is not defined                                |
| `default` response                       | Supported. Used as fallback when neither an exact status code nor a status code range is defined |
| Response headers                         | Supported                                                                                        |
| Response body                            | Supported                                                                                        |
| Links                                    | Not supported                                                                                    |

---

## Next Steps

- [Troubleshooting](../guides/troubleshooting.md) -- common issues caused by unsupported features and how to work around them.
- [Creating Scenarios](scenarios.md) -- how OpenAPI examples become scenarios and verification cases.
- [Testing Your Server](testing-your-server.md) -- what the verifier checks.
- [Testing Your Client](testing-your-client.md) -- how the mock server validates requests and generates responses.
