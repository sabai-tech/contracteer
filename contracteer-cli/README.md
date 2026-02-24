# contracteer-cli

Run the Contracteer verifier or mock server from the command
line. Works with any language or stack, integrates into CI/CD
pipelines.

## Installation

### macOS / Linux (Homebrew)

```bash
brew install sabai-tech/contracteer/contracteer
```

### Linux / Windows / macOS (GitHub Releases)

Download the archive for your platform from the
[latest release](https://github.com/sabai-tech/contracteer/releases/latest)
and extract it.

## Commands

The OpenAPI specification can be a local file path or an
HTTP(S) URL.

### `contracteer verify`

Verify that a running server implements its OpenAPI
specification. Exits with code 0 if all cases pass, 1
otherwise.

```bash
contracteer verify openapi.yaml --server-url http://localhost --server-port 8080
```

| Option | Default | Description |
|--------|---------|-------------|
| `-u`, `--server-url` | `http://localhost` | Server base URL. |
| `-p`, `--server-port` | `8080` | Server port. |
| `-l`, `--log-level` | `INFO` | Log verbosity: TRACE, DEBUG, INFO, WARN, ERROR, OFF. |
| `-t`, `--http-traffic` | off | Enable HTTP request/response logging. |

### `contracteer mock`

Start a mock server that validates requests and returns
spec-compliant responses. Runs until terminated.

```bash
contracteer mock openapi.yaml --port 9090
```

| Option | Default | Description |
|--------|---------|-------------|
| `-p`, `--port` | `8080` | Port for the mock server. |
| `-l`, `--log-level` | `INFO` | Log verbosity: TRACE, DEBUG, INFO, WARN, ERROR, OFF. |
| `-t`, `--http-traffic` | off | Enable HTTP request/response logging. |
