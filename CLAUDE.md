# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> **IMPORTANT**: This project is for **learning purposes only**. Do not use in production environments.

## Project Overview

This is a Java port of [Microsoft Agent Framework (Python)](https://github.com/microsoft/agent-framework/tree/main/python). It provides a framework for building AI agents that interact with LLMs (OpenAI, Anthropic).

## Module Structure

This project uses a multi-module Gradle structure:

| Module | Description |
|--------|-------------|
| **core** | Main framework with all source code |
| **samples** | Example code demonstrating usage |
| **agui** | AGUI (Agent Graphical User Interface) integration with agent-framework |
| **agui-core** | Core AGUI components - event system, message types, agent interfaces |
| **agui-example** | Vert.x-based web server example with frontend |

## Build Commands

```bash
./gradlew build        # Compile and run all tests
./gradlew test         # Run JUnit 5 tests only
./gradlew clean        # Clean build outputs
./gradlew :core:classes     # Compile core main classes only
./gradlew :core:testClasses # Compile core test classes only
```

Run a single test:
```bash
./gradlew test --tests "github.ponyhuang.agentframework.FunctionToolTest"
```

## Architecture

### Key Interfaces

- **Agent**: Main entry point with `run()` and `runStream()` methods; `BaseAgent` provides common functionality
- **ChatClient**: LLM provider abstraction with `chat()` and `chatStream()` methods
- **Tool**: Tool definition interface; use `@Tool` and `@Param` annotations for function calling
- **Hooks**: Lifecycle event system in `hooks` package - CommandHookHandler, PromptHookHandler, HttpHookHandler for agent customization
- **AgentSession**: Conversation state with HistoryProvider and ContextProvider
- **Workflows**: Graph-based workflow engine for complex agent orchestration

### Execution Flow

1. `agent.run(messages)` is called
2. `BaseAgent.run()` invokes the HookExecutor to process registered hooks
3. `doRun()` is called, which calls the ChatClient
4. Tool calls are handled - LLM can request function calls, results are fed back
5. Final response is returned

## Testing

- Uses JUnit 5 (`@Test`, `@BeforeEach`, etc.)
- Uses Mockito for mocking (`@Mock`, `when()`, `verify()`)
- Test files in `core/src/test/java/`

## Dependencies

- **LLM Providers**: anthropic-java 2.13.0, openai-java 4.22.0
- **Reactive**: reactor-core 3.8.2
- **Observability**: opentelemetry-api 1.47.0, opentelemetry-sdk 1.47.0
- **MCP**: io.modelcontextprotocol.sdk:mcp 1.0.0
- **Logging**: logback-classic 1.5.18

## Development Notes

This project was developed using **Vibe Coding** and **Spec-Driven** approaches:
- **Vibe Coding**: Implemented based on intuitive understanding and rapid prototyping without strict test-driven development
- **Spec-Driven**: Uses OpenSpec for artifact management and implementation tracking

Due to this development approach, the codebase may have:
- Untested edge cases
- Incomplete error handling
- Potential memory leaks
- Race conditions in concurrent code
- Inconsistent code patterns