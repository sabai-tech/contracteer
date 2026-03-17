# contracteer-cli

Run the Contracteer verifier or mock server from the command line.
Works with any language or stack -- no JVM required.

## Installation

```bash
brew install sabai-tech/contracteer/contracteer
```

Or download the archive for your platform from the [latest release](https://github.com/sabai-tech/contracteer/releases/latest).

## Usage

Verify a running server:

```bash
contracteer verify openapi.yaml
```

Start a mock server:

```bash
contracteer mock openapi.yaml
```

The specification can be a local file path or an HTTP(S) URL.

## Documentation

See [Use the CLI](https://sabai-tech.github.io/contracteer/getting-started/cli/) for the full guide -- all options, example output, mock server behavior, and a try-it-now example.