# XLR

A space for XLR type definitions and converters for every supported language.

## About

Based off of grantila's [core-types](https://github.com/grantila/core-types) library with some modifications to support a few more types present in Player as well as generics, these packages allow for the generation of language agnostic representations of the various capabilities that can get plugged into Player. This enables those capabilities to be read/parsed/used outside of Player for functionality like validation, content authoring and cross platform code generation. The packages in this folder contain the definitions of these types, converters for generating them, and utilities for interacting with them.

## Packages

### Types

Language-specific XLR type definitions that describe the XLR AST (Abstract Syntax Tree) node shapes.

- **`types/javascript`** (`@xlr-lib/xlr`) — Core TypeScript/JavaScript type definitions for all XLR node types, including primitives (`string`, `number`, `boolean`, etc.), complex types (`object`, `array`, `union`, `intersection`, `conditional`, `ref`, `template-literal`), generics, and annotation types.
- **`types/python`** — Python equivalents of the XLR type definitions (in development). Includes node classes, type guards, and JSON deserialization support.

### Converters

- **`converters`** (`@xlr-lib/xlr-converters`) — Bidirectional converters between TypeScript and XLR. Parses TypeScript source files and converts them to XLR JSON, and generates TypeScript type definitions from XLR. Includes a CLI (`xlr`) for running conversions, manifest generation, and JSDoc/annotation preservation.

### SDK

- **`sdk`** (`@xlr-lib/xlr-sdk`) — Unified interface for working with XLRs. Handles loading XLR manifests from the filesystem, exporting types to TypeScript, validating JSON payloads against XLR schemas, and managing types via an extensible registry system.

### Utils

- **`utils`** (`@xlr-lib/xlr-utils`) — Shared utilities for traversing, manipulating, and type-checking XLR nodes. Key functions include generic resolution (`fillInGenerics`), extends computation (`computeExtends`), reference resolution (`resolveReferenceNode`), and conditional type resolution (`resolveConditional`).

### Helpers

- **`helpers/static-xlrs`** (`@xlr-lib/static-xlrs`) — Pre-compiled XLR manifests for Player framework core types. Provides ready-to-use JSON type definitions for Player core types, common expressions, and reference asset plugins.
- **`helpers/test-utils`** (`@xlr-lib/test-utils`) — Testing utilities and helpers shared across XLR packages.

## Architecture

```
TypeScript Source Code
        │
        ▼
   xlr-converters        (TypeScript → XLR)
        │
        ▼
  XLR JSON + Manifest
        │
        ├──▶  xlr-sdk    (Load / Validate / Export)
        │
        └──▶  static-xlrs (pre-compiled Player types)
```

## Getting Started

This repo uses [Bazel](https://bazel.build/) for builds and [pnpm](https://pnpm.io/) for package management.

```bash
# Install dependencies
pnpm install

# Build all packages
bazel build //...

# Run all tests
bazel test //...
```
