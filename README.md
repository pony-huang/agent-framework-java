# agent-framework-java

> **IMPORTANT**: This project is for **learning purposes only**. Do not use in production environments.

## Overview

agent-framework-java is a Java port of [Microsoft Agent Framework (Python)](https://github.com/microsoft/agent-framework) and a re-implementation inspired by [ByteDance Trae Agent](https://github.com/bytedance/trae-agent). This project is designed to provide a framework for building AI agents that interact with Large Language Models (LLMs) such as OpenAI and Anthropic.

This is an educational project created for learning purposes. It demonstrates how to build LLM-powered agent systems in Java, implementing core concepts from the original Python projects.

## Re-implementation Sources

This project re-implements concepts and patterns from:

- [Microsoft Agent Framework (Python)](https://github.com/microsoft/agent-framework) - The original agent framework
- [ByteDance Trae Agent](https://github.com/bytedance/trae-agent) - A powerful software engineering agent

## Project Structure

```
agent-framework-java/
├── src/main/java/github/ponyhuang/agentframework/
│   ├── agents/          # Agent core interfaces and classes
│   ├── clients/         # Chat client interfaces
│   ├── providers/      # LLM provider implementations
│   ├── tools/          # Tool system for function calling
│   ├── middleware/     # Middleware for request/response processing
│   ├── sessions/       # Session management for conversation state
│   ├── workflows/      # Workflow engine for complex task orchestration
│   ├── mcp/            # Model Context Protocol support
│   ├── orchestrations/ # Multi-agent patterns
│   ├── types/          # Data types and models
│   └── observability/  # OpenTelemetry tracing support
├── samples/            # Example code demonstrating usage
└── trae-agent-java/    # Complete Trae Agent implementation demo
```

## Module Details

### 1. agents/ - Agent Core

**Function**: Provides the core abstraction for building AI agents.

- `Agent.java` - Main interface defining agent contract
- `BaseAgent.java` - Abstract base class with common functionality
- `AgentBuilder.java` - Builder for constructing agents

### 2. clients/ - Chat Client Interfaces

**Function**: Defines interfaces for communicating with LLM providers.

- `ChatClient.java` - Interface for chat completion
- `StreamingChatClient.java` - Interface for streaming responses
- `EmbeddingClient.java` - Interface for embedding generation

### 3. providers/ - LLM Provider Implementations

**Function**: Provides concrete implementations for different LLM providers.

- `OpenAIChatClient.java` - OpenAI GPT model support
- `AnthropicChatClient.java` - Anthropic Claude model support

### 4. tools/ - Tool System

**Function**: Enables function calling capabilities for agents.

- `Tool.java` - Annotation for defining tools
- `FunctionTool.java` - Tool implementation for function calls
- `ToolExecutor.java` - Executes tool calls from LLM responses

### 5. middleware/ - Middleware System

**Function**: Provides hooks for processing requests and responses at different layers.

- `AgentMiddleware.java` - Wraps entire agent execution
- `ChatMiddleware.java` - Wraps LLM chat calls
- `FunctionMiddleware.java` - Wraps tool executions

### 6. sessions/ - Session Management

**Function**: Manages conversation state and history.

- `AgentSession.java` - Interface for session management
- `HistoryProvider.java` - Provides conversation history
- `ContextProvider.java` - Provides additional context

### 7. workflows/ - Workflow Engine

**Function**: Supports complex task orchestration through graph-based workflows.

- `Workflow.java` - Workflow definition
- `WorkflowBuilder.java` - Builder for constructing workflows
- `WorkflowExecutor.java` - Executes workflows

### 8. mcp/ - Model Context Protocol

**Function**: Provides MCP (Model Context Protocol) tool support.

- `MCPTool.java` - Base class for MCP tools
- `MCPStdioTool.java` - MCP over stdio
- `MCPStreamableHTTPTool.java` - MCP over HTTP

### 9. orchestrations/ - Multi-Agent Patterns

**Function**: Provides patterns for coordinating multiple agents.

- `SequentialAgentBuilder.java` - Sequential agent execution
- `ConcurrentAgentBuilder.java` - Concurrent agent execution
- `HandoffAgentBuilder.java` - Agent handoff pattern
- `GroupChatAgentBuilder.java` - Group chat pattern
- `MagenticAgentBuilder.java` - Magentic pattern

### 10. types/ - Data Types

**Function**: Core data types for the framework.

- `Message.java` - Conversation message
- `ChatResponse.java` - Chat completion response
- `ChatCompleteParams.java` - Request parameters

### 11. observability/ - Observability

**Function**: Provides OpenTelemetry tracing support.

- `TracingMiddleware.java` - Tracing middleware for monitoring

## Submodules

### samples/

Contains example code demonstrating various features of the framework:

- `SimpleAgentExample.java` - Basic agent usage
- `MultiTurnExample.java` - Multi-turn conversation
- `ToolUsageExample.java` - Tool calling
- `McpToolExample.java` - MCP tools
- `MiddlewareExample.java` - Custom middleware
- `WorkflowExample.java` - Workflow usage

### trae-agent-java/

A complete implementation of ByteDance's Trae Agent in Java, demonstrating:

- TraeAgent core implementation
- BashTool, EditTool, JSONEditTool
- SequentialThinkingTool, TaskDoneTool
- YAML configuration
- Trajectory recording
- CLI interface

## License

This project is for educational purposes only. See the original projects for their respective licenses.

## Disclaimer

This is NOT production-ready software. It is a learning project that demonstrates AI agent concepts in Java. Do not use this in any production environment.

### Development Methodology

This project was developed using **Vibe Coding** and **Spec-Driven** approaches:

- **Vibe Coding**: Implemented based on intuitive understanding and rapid prototyping without strict test-driven development
- **Spec-Driven**: Uses OpenSpec for artifact management and implementation tracking

### Known Issues

Due to the development approach, this project may contain:
- Untested edge cases
- Incomplete error handling
- Potential memory leaks
- Race conditions in concurrent code
- Inconsistent code patterns

**Use at your own risk and only for learning purposes.**