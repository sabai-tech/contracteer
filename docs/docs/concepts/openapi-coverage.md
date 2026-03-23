# OpenAPI 3.0 Coverage

Contracteer supports OpenAPI 3.0.x.
OpenAPI 3.1 is not yet supported.

This page lists which features of the specification are supported and which are not yet supported.
It also explains what happens when your specification uses an unsupported feature.

---

## What Happens with Unsupported Features

Contracteer does not reject a specification because it contains unsupported features.
Instead, it handles them gracefully:

- **Unsupported content types** (XML, multipart, form-urlencoded) -- the operation is skipped with a warning.
- **Unsupported parameter types** (array or object parameters) -- the operation is skipped with a warning.
- **Unsupported schema keywords** (pattern, minItems, etc.) -- the keyword is ignored.
  Values are generated and validated without that constraint.

Your specification still loads.
Only the affected operations or constraints are skipped.

---

## Data Types

| Type | Status | Notes |
|------|--------|-------|
| `string` | Supported | With `minLength`, `maxLength`, `enum`, `nullable` |
| `integer` | Supported | With `minimum`, `maximum`, `exclusiveMinimum`, `exclusiveMaximum`, `enum`, `nullable` |
| `number` | Supported | With `minimum`, `maximum`, `exclusiveMinimum`, `exclusiveMaximum`, `enum`, `nullable` |
| `boolean` | Supported | With `enum`, `nullable` |
| `array` | Supported (body only) | Array parameters are not yet supported -- operations using them are skipped |
| `object` | Supported (body only) | Object parameters are not yet supported -- operations using them are skipped |

---

## String Formats

| Format | Status |
|--------|--------|
| `date` | Supported (ISO 8601 `YYYY-MM-DD`) |
| `date-time` | Supported (ISO 8601 with offset) |
| `email` | Supported |
| `uuid` | Supported |
| `byte` (base64) | Supported |
| `binary` | Supported |
| `password` | Supported (treated as plain string) |
| `int32` / `int64` | Not enforced (parsed as integer, no range constraint) |
| `float` / `double` | Not enforced (parsed as number, no precision constraint) |
| Custom formats | Ignored |

---

## Schema Keywords

### Supported

| Keyword | Notes |
|---------|-------|
| `nullable` | All data types |
| `enum` | All data types |
| `minimum` / `maximum` | Integer and number types |
| `exclusiveMinimum` / `exclusiveMaximum` | Integer and number types (OAS 3.0 boolean semantics) |
| `minLength` / `maxLength` | String, email, base64, binary types |
| `required` (properties) | Object types |
| `additionalProperties` | Both boolean and typed schema forms |
| `discriminator` | With `propertyName` and `mapping` on allOf, oneOf, anyOf |
| `readOnly` / `writeOnly` | readOnly properties excluded from request schemas, writeOnly from response schemas |

### Not yet supported

| Keyword | Impact |
|---------|--------|
| `pattern` | No regex validation or generation for string values |
| `multipleOf` | No divisibility constraint on numbers |
| `minItems` / `maxItems` | No array length constraints. Generated arrays contain 1-5 items |
| `uniqueItems` | No uniqueness enforcement on array items |
| `minProperties` / `maxProperties` | No property count constraints on objects |
| `default` (property values) | Default values are not used for generation |
| `not` | Schema negation is not supported |

---

## Schema Composition

| Feature | Status |
|---------|--------|
| `allOf` | Supported. Contracteer requires all sub-schemas to be structured types (object or composite) |
| `oneOf` | Supported. Validates that exactly one sub-schema matches |
| `anyOf` | Supported. Validates that at least one sub-schema matches |
| `discriminator` | Supported on allOf, oneOf, anyOf with `propertyName` and `mapping` |
| `not` | Not supported |

---

## Parameters

| Feature | Status |
|---------|--------|
| `in: path` | Supported (primitive types only) |
| `in: query` | Supported (primitive types only) |
| `in: header` | Supported (primitive types only) |
| `in: cookie` | Supported (primitive types only) |
| Array / object parameters | Not supported. Operations using them are skipped. Planned |
| `style` / `explode` | Not supported. Planned |
| `content` (instead of `schema`) | Not supported |
| `allowEmptyValue` | Not supported |
| `allowReserved` | Not supported |

---

## Request and Response Bodies

| Feature | Status |
|---------|--------|
| `application/json` | Supported |
| Plain text content types | Supported |
| `application/xml` | Not supported. Operations using it are skipped |
| `multipart/form-data` | Not supported. Operations using it are skipped |
| `application/x-www-form-urlencoded` | Not supported. Operations using it are skipped. Planned |
| Multiple content types | Supported. Produces a cartesian product of verification cases |
| `required` (request body) | Supported |

---

## Examples and Scenarios

| Feature | Status |
|---------|--------|
| `examples` map (parameter / media type level) | Supported. Core mechanism for scenario creation |
| Single `example` (parameter / media type level) | Supported |
| Status-code-prefixed example keys | Supported (`404_NOT_FOUND`, `400_INVALID_INPUT`) |
| `example` on schema properties | Not used for scenario creation |
| `externalValue` in Example Objects | Not supported. Only inline `value` is read |

See [Creating Scenarios](scenarios.md) for how examples drive scenario creation.

---

## References ($ref)

| Feature | Status |
|---------|--------|
| `$ref` to `#/components/schemas` | Supported |
| `$ref` to `#/components/parameters` | Supported |
| `$ref` to `#/components/requestBodies` | Supported |
| `$ref` to `#/components/responses` | Supported |
| `$ref` to `#/components/headers` | Supported |
| `$ref` to `#/components/examples` | Supported |
| Recursive / chained `$ref` | Supported (with depth limits) |
| External `$ref` (other files) | Supported (resolved by the OpenAPI parser before Contracteer processes the model) |

---

## Responses

| Feature | Status |
|---------|--------|
| Exact status codes (`200`, `404`, etc.) | Supported |
| Status code ranges (`2XX`, `4XX`, `5XX`) | Supported. Used as fallback when exact status code is not defined |
| `default` response | Supported. Used as fallback when neither an exact status code nor a status code range is defined |
| Response headers | Supported |
| Response body | Supported |
| Links | Not supported |

---

## Next Steps

- [Troubleshooting](../guides/troubleshooting.md) -- common issues caused by unsupported features and how to work around them.
- [Creating Scenarios](scenarios.md) -- how OpenAPI examples become scenarios and verification cases.
- [Testing Your Server](testing-your-server.md) -- what the verifier checks.
- [Testing Your Client](testing-your-client.md) -- how the mock server validates requests and generates responses.
