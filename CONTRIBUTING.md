# Contributing to Contracteer

Thanks for your interest in contributing to **Contracteer**! 🎉  
Whether you're fixing a bug, improving the documentation, or proposing a new feature — your help is appreciated.

Contracteer is a developer-first toolkit for contract testing using OpenAPI 3. It emphasizes fast, isolated, and
reliable testing early in the development lifecycle. Contributions should align with this mission.

---

## 🚀 How to Contribute

1. **Fork** the repository
2. **Create a feature branch**
   ```bash
   git checkout -b feat/my-feature-name
   ```
3. **Make your changes**, including tests if applicable
4. **Write commits using [Conventional Commits](https://www.conventionalcommits.org/)**
   ```bash
   git commit -am "feat(verifier): add support for optional headers"
   ```
5. **Push** to your fork
6. **Open a Pull Request** and describe your changes clearly

---

## 📦 Project Modules

| Module                                         | Description                                                                                                     |
|------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| **contracteer-core**                           | Core functionalities and OpenAPI spec parsing                                                                   |
| **contracteer-cli**                            | CLI engine for running mock servers and verifications                                                           |
| **contracteer-mockserver**                     | Mock server generation for contract-based testing                                                               |
| **contracteer-mockserver-spring-boot-starter** | Seamless mock server integration with Spring Boot                                                               |
| **contracteer-verifier**                       | Automated verification that compares actual API responses to expected behaviors defined in the OpenAPI Document |
| **contracteer-verifier-junit**                 | JUnit 5 integration for automated contract verification tests                                                   |

---


Core validation engine that compares actual API responses to expected behaviors defined in the OpenAPI contract |

## 🛠 Development Setup

### Requirements

- Java 21+
- Kotlin 2.x
- Gradle (or use the Gradle Wrapper)

### Build the project

```bash
./gradlew build
```

### Run tests

```bash
./gradlew test
```

### Run the CLI locally

```bash
./gradlew :contracteer-cli:nativeCompile
./contracteer-cli/build/native/nativeCompile/contracteer --help
```

---

## 🎨 Code Style & Quality

- Follow Kotlin conventions for formatting and structure
- Make sure your code builds and tests pass before pushing
- Add or update tests for your changes where applicable

---

## 📝 Commit Message Guidelines

We follow the [Conventional Commits](https://www.conventionalcommits.org/) standard to keep the commit history clean and
automate changelog/versioning.

Examples:

```bash
feat(mockserver): support multiple mock server ports
fix(verifier): correct null handling in optional params
docs: update README with Maven usage
```

---

## 📥 Pull Request Guidelines

- Keep changes focused and small
- One feature or bugfix per PR
- Include tests if relevant
- Update documentation or README if behavior changes
- Reference related issues (e.g., `Closes #42`)

---

## 🐞 Reporting Bugs

Please include the following details when filing an issue:

- What you were trying to do
- The exact error message/output
- Steps to reproduce
- A relevant OpenAPI snippet (if applicable)
- Contracteer version (CLI or library)

---

## 🤝 Code of Conduct

Please review our [Code of Conduct](CODE_OF_CONDUCT.md) to ensure a welcoming and respectful environment for everyone.

---

Thanks again for helping improve Contracteer! 🚀
