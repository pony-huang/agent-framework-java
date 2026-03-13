# agent-framework-java

**[中文](./README_ZH.md)** | English

> **IMPORTANT**: This project is for **learning purposes only**. Do not use in production environments.

Java port of [Microsoft Agent Framework](https://github.com/microsoft/agent-framework) - a framework for building AI agents that interact with LLMs.

## Modules

| Module | Description |
|--------|-------------|
| **core** | Main framework - agents, clients, tools, middleware, sessions, workflows, MCP, orchestrations |
| **samples** | Example code |
| **agui** | AGUI integration with agent-framework |
| **agui-core** | AGUI core - events, messages, agent interfaces |
| **agui-example** | Vert.x web server example |

## Core Packages

| Package | Description |
|---------|-------------|
| **agents** | Agent interface + BaseAgent; entry point is `run()` and `runStream()` |
| **clients** | ChatClient interface for LLM communication |
| **providers** | OpenAI and Anthropic client implementations |
| **tools** | Tool system for function calling via `@Tool` and `@Param` annotations |
| **middleware** | Three-layer middleware: Agent, Chat, Function |
| **sessions** | Conversation state management with HistoryProvider, ContextProvider |
| **workflows** | Graph-based workflow engine |
| **mcp** | MCP tool support - stdio and HTTP transport |
| **orchestrations** | Multi-agent patterns: Sequential, Concurrent, Handoff, GroupChat, Magentic |
| **types** | Core data types - Message, Content, Role, ChatResponse |
| **observability** | OpenTelemetry tracing middleware |