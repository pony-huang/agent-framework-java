# agent-framework-java

> **重要提示**: 此项目仅供**学习使用**，请勿在生产环境中使用。

## 项目概述

agent-framework-java 是 [Microsoft Agent Framework (Python)](https://github.com/microsoft/agent-framework) 的 Java 移植版本，同时参考了 [ByteDance Trae Agent](https://github.com/bytedance/trae-agent) 进行重新实现。该项目旨在为构建与大型语言模型 (LLM)（如 OpenAI 和 Anthropic）交互的 AI 代理提供框架。

这是一个教育目的的学习项目，旨在演示如何在 Java 中构建 LLM 驱动的代理系统，实现原始 Python 项目中的核心概念。

## 重新实现来源

本项目重新实现以下项目的概念和模式：

- [Microsoft Agent Framework (Python)](https://github.com/microsoft/agent-framework) - 原始代理框架
- [ByteDance Trae Agent](https://github.com/bytedance/trae-agent) - 强大的软件工程代理

## 项目结构

```
agent-framework-java/
├── src/main/java/github/ponyhuang/agentframework/
│   ├── agents/          # 代理核心接口和类
│   ├── clients/         # 聊天客户端接口
│   ├── providers/       # LLM 提供商实现
│   ├── tools/           # 函数调用工具系统
│   ├── middleware/      # 中间件用于请求/响应处理
│   ├── sessions/       # 会话管理，用于对话状态
│   ├── workflows/      # 工作流引擎，用于复杂任务编排
│   ├── mcp/            # 模型上下文协议支持
│   ├── orchestrations/ # 多代理模式
│   ├── types/          # 数据类型和模型
│   └── observability/  # OpenTelemetry 追踪支持
├── samples/            # 演示用法的示例代码
└── trae-agent-java/    # 完整的 Trae Agent 实现演示
```

## 模块详情

### 1. agents/ - 代理核心

**功能**: 提供构建 AI 代理的核心抽象。

- `Agent.java` - 定义代理契约的主要接口
- `BaseAgent.java` - 具有通用功能的抽象基类
- `AgentBuilder.java` - 用于构建代理的构建器

### 2. clients/ - 聊天客户端接口

**功能**: 定义与 LLM 提供商通信的接口。

- `ChatClient.java` - 聊天完成接口
- `StreamingChatClient.java` - 流式响应接口
- `EmbeddingClient.java` - 向量嵌入生成接口

### 3. providers/ - LLM 提供商实现

**功能**: 为不同 LLM 提供商提供具体实现。

- `OpenAIChatClient.java` - OpenAI GPT 模型支持
- `AnthropicChatClient.java` - Anthropic Claude 模型支持

### 4. tools/ - 工具系统

**功能**: 为代理启用函数调用能力。

- `Tool.java` - 定义工具的注解
- `FunctionTool.java` - 函数调用的工具实现
- `ToolExecutor.java` - 执行 LLM 响应中的工具调用

### 5. middleware/ - 中间件系统

**功能**: 提供在不同层处理请求和响应的钩子。

- `AgentMiddleware.java` - 包装整个代理执行
- `ChatMiddleware.java` - 包装 LLM 聊天调用
- `FunctionMiddleware.java` - 包装工具执行

### 6. sessions/ - 会话管理

**功能**: 管理对话状态和历史。

- `AgentSession.java` - 会话管理接口
- `HistoryProvider.java` - 提供对话历史
- `ContextProvider.java` - 提供额外上下文

### 7. workflows/ - 工作流引擎

**功能**: 通过基于图形的工作流支持复杂任务编排。

- `Workflow.java` - 工作流定义
- `WorkflowBuilder.java` - 构建工作流的构建器
- `WorkflowExecutor.java` - 执行工作流

### 8. mcp/ - 模型上下文协议

**功能**: 提供 MCP (Model Context Protocol) 工具支持。

- `MCPTool.java` - MCP 工具基类
- `MCPStdioTool.java` - 通过 stdio 的 MCP
- `MCPStreamableHTTPTool.java` - 通过 HTTP 的 MCP

### 9. orchestrations/ - 多代理模式

**功能**: 提供协调多个代理的模式。

- `SequentialAgentBuilder.java` - 顺序代理执行
- `ConcurrentAgentBuilder.java` - 并发代理执行
- `HandoffAgentBuilder.java` - 代理交接模式
- `GroupChatAgentBuilder.java` - 群聊模式
- `MagenticAgentBuilder.java` - Magentic 模式

### 10. types/ - 数据类型

**功能**: 框架的核心数据类型。

- `Message.java` - 对话消息
- `ChatResponse.java` - 聊天完成响应
- `ChatCompleteParams.java` - 请求参数

### 11. observability/ - 可观测性

**功能**: 提供 OpenTelemetry 追踪支持。

- `TracingMiddleware.java` - 用于监控的追踪中间件

## 子模块

### samples/

包含演示框架各种功能的示例代码：

- `SimpleAgentExample.java` - 基础代理用法
- `MultiTurnExample.java` - 多轮对话
- `ToolUsageExample.java` - 工具调用
- `McpToolExample.java` - MCP 工具
- `MiddlewareExample.java` - 自定义中间件
- `WorkflowExample.java` - 工作流用法

### trae-agent-java/

一个完整的 ByteDance Trae Agent 的 Java 实现，演示：

- TraeAgent 核心实现
- BashTool、EditTool、JSONEditTool
- SequentialThinkingTool、TaskDoneTool
- YAML 配置
- 轨迹记录
- CLI 接口

## 许可证

此项目仅供学习使用。许可证请参阅原始项目。

## 免责声明

这是**非生产就绪**软件。它是一个演示 AI 代理概念的学习项目。请勿在任何生产环境中使用。

### 开发方法论

本项目采用 **Vibe Coding** 和 **Spec-Driven** 方法开发：

- **Vibe Coding**: 基于直觉理解和快速原型开发，没有严格的测试驱动开发
- **Spec-Driven**: 使用 OpenSpec 进行制品管理和实现跟踪

### 已知问题

由于开发方法论的限制，本项目可能存在：
- 未测试的边界情况
- 不完整的错误处理
- 潜在的内存泄漏
- 并发代码中的竞态条件
- 不一致的代码模式

**使用风险自负，仅供学习目的使用。**