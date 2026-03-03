# agent-framework-java

**[English](./README.md)** | 中文

> **重要**: 此项目仅供**学习使用**,请勿用于生产环境.

[Microsoft Agent Framework](https://github.com/microsoft/agent-framework) 的 Java 移植版 - 构建与 LLM 交互的 AI Agent 框架。

## 模块

| 模块 | 描述 |
|------|------|
| **core** | 核心框架 - agents, clients, tools, middleware, sessions, workflows, MCP, orchestrations |
| **samples** | 示例代码 |
| **trae-agent-java** | Trae Agent 实现示例 |
| **agui** | AGUI 与 agent-framework 集成 |
| **agui-core** | AGUI 核心 - events, messages, agent interfaces |
| **agui-example** | Vert.x Web 服务器示例 |

## 核心包

| 包 | 描述 |
|----|------|
| **agents** | Agent 接口 + BaseAgent; 入口方法 `run()` 和 `runStream()` |
| **clients** | LLM 通信接口 ChatClient |
| **providers** | OpenAI 和 Anthropic 客户端实现 |
| **tools** | 函数调用系统,使用 `@Tool` 和 `@Param` 注解 |
| **middleware** | 三层中间件: Agent, Chat, Function |
| **sessions** | 对话状态管理,含 HistoryProvider, ContextProvider |
| **workflows** | 基于图的 workflow 引擎 |
| **mcp** | MCP 工具支持 - stdio 和 HTTP 传输 |
| **orchestrations** | 多 Agent 模式: Sequential, Concurrent, Handoff, GroupChat, Magentic |
| **types** | 核心数据类型 - Message, Content, Role, ChatResponse |
| **observability** | OpenTelemetry 追踪中间件 |