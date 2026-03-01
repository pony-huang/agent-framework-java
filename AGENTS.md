# Repository Guidelines

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Java port of [Microsoft Agent Framework (Python)](https://github.com/microsoft/agent-framework/tree/main/python). It provides a framework for building AI agents that interact with LLMs (OpenAI, Anthropic).

## Module Structure

This project uses a multi-module Gradle structure:

| Module | Description |
|--------|-------------|
| **core** | Main framework with all source code |
| **samples** | Example code demonstrating usage |
| **trae-agent-java** | Complete Trae Agent implementation demo |

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
./gradlew test --tests "tools.github.ponyhuang.agentframework.FunctionToolTest"
```

## Architecture

### Core Components

| Layer | Description |
|-------|-------------|
| **Agents** | `Agent` interface + `BaseAgent` abstract class; entry point is `run()` which supports both sync and streaming (`runStream`) |
| **Clients** | `ChatClient` interface for LLM communication; implementations: `OpenAIChatClient`, `AnthropicChatClient` |
| **Tools** | `Tool` interface, `FunctionTool` for function calling, `ToolExecutor` for execution; uses `@Tool` and `@Param` annotations |
| **Middleware** | Three-layer middleware: `AgentMiddleware`, `ChatMiddleware`, `FunctionMiddleware` |
| **Sessions** | `AgentSession` for conversation state; `HistoryProvider`, `ContextProvider` for history/context |
| **Workflows** | Graph-based workflow engine with `WorkflowBuilder` and `WorkflowExecutor` |
| **MCP** | Model Context Protocol support via `MCPTool`, `MCPStdioTool`, `MCPStreamableHTTPTool` |
| **Orchestrations** | Multi-agent patterns: `SequentialAgentBuilder`, `ConcurrentAgentBuilder`, `HandoffAgentBuilder`, `GroupChatAgentBuilder`, `MagenticAgentBuilder` |

### Package Structure (in core module)

```
core/src/main/java/github/ponyhuang/agentframework/
├── agents/        # Agent, BaseAgent, AgentBuilder
├── clients/       # ChatClient, EmbeddingClient
├── providers/     # OpenAIChatClient, AnthropicChatClient
├── tools/         # Tool, FunctionTool, ToolExecutor, @Tool, @Param
├── middleware/    # AgentMiddleware, ChatMiddleware, FunctionMiddleware
├── sessions/      # AgentSession, HistoryProvider, ContextProvider
├── workflows/     # Workflow, WorkflowBuilder, WorkflowExecutor
├── mcp/           # MCPTool, MCPStdioTool, MCPStreamableHTTPTool
├── orchestrations/ # Multi-agent builders
├── types/         # Message, Content, Role, ChatResponse, ChatCompleteParams
└── observability/ # TracingMiddleware (OpenTelemetry)
```

### Key Interfaces

- **Agent**: Main entry point with `run()` and `runStream()` methods
- **ChatClient**: LLM provider abstraction with `chat()` and `chatStream()` methods
- **Tool**: Tool definition interface
- **Middleware**: Three levels - Agent (wraps entire run), Chat (wraps LLM call), Function (wraps tool execution)

### Execution Flow

1. `agent.run(messages)` is called
2. `BaseAgent.run()` executes middleware chain (if any)
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
