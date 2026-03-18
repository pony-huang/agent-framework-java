# Feature Matrix: Python SDK vs Java Implementation

This document compares the features of `claude-agent-sdk-python` with the Java agent framework implementation.

## Overview

| Feature Category | Python SDK | Java Implementation | Status |
|-----------------|------------|---------------------|--------|
| Core Agent | Implemented | Implemented | ✅ Complete |
| Tool System | Implemented | Implemented | ✅ Complete |
| Hook System | Implemented | Implemented | ✅ Complete (Enhanced) |
| Session Management | Implemented | Partial | ⚠️ Partial |
| Agent Configuration | Partial | Partial | ⚠️ Partial |
| Custom Agents | Implemented | Not Implemented | ❌ Missing |

---

## Detailed Feature Comparison

### Core Agent APIs

| Python SDK Feature | Java Implementation | Status | Notes |
|-------------------|---------------------|--------|-------|
| `query()` | `LoopAgent.run()` | ✅ | One-shot query |
| `ClaudeSDKClient` | `LoopAgent + Session` | ✅ | Bidirectional conversation |
| `ClaudeAgentOptions` | `AgentBuilder` | ⚠️ Partial | Missing some options |
| `AgentDefinition` | Not implemented | ❌ | Custom sub-agents |

### Agent Configuration Options

| Python SDK Option | Java Builder Method | Status |
|------------------|---------------------|--------|
| `allowed_tools` | `allowedTools(Set)` | ✅ Implemented |
| `disallowed_tools` | `disallowedTools(Set)` | ✅ Implemented |
| `system_prompt` | `instructions(String)` | ✅ Implemented |
| `permission_mode` | `permissionMode(PermissionMode)` | ✅ Implemented |
| `max_turns` | `maxSteps(int)` | ✅ Implemented |
| `max_budget_usd` | `maxBudgetUsd(double)` | ✅ Implemented |
| `model` | `client.getModel()` | ✅ Implemented |
| `fallback_model` | `fallbackModel(String)` | ✅ Implemented |
| `cwd` | `workingDirectory(String)` | ✅ Implemented |
| `env` | Through Session metadata | ⚠️ Partial |
| `continue_conversation` | Session management | ✅ Implemented |
| `resume` | Not implemented | ❌ Missing |
| `fork_session` | Not implemented | ❌ Missing |
| `thinking` | Not implemented | ❌ N/A |
| `effort` | Not implemented | ❌ N/A |
| `include_partial_messages` | Not implemented | ❌ Missing |
| `enable_file_checkpointing` | Not implemented | ❌ N/A |
| `add_dirs` | Not implemented | ❌ Missing |
| `sandbox` | N/A | N/A | Not applicable (API-based) |

### Hook System

| Python SDK Hook | Java Hook Event | Status |
|-----------------|-----------------|--------|
| `PreToolUse` | `PRE_TOOL_USE` | ✅ Implemented |
| `PostToolUse` | `POST_TOOL_USE` | ✅ Implemented |
| `HookMatcher` | Hook registration with matcher | ✅ Implemented |
| Permission events | `PERMISSION_REQUEST` | ✅ Implemented |
| Session events | `SESSION_START`, `SESSION_END` | ✅ Implemented |
| Custom events | 15+ additional events | ✅ Enhanced |

**Java-only Hook Events**:
- `STOP`
- `NOTIFICATION`
- `SUBAGENT_START`
- `SUBAGENT_STOP`
- `TASK_COMPLETED`
- `CONFIG_CHANGE`
- `PRE_COMPACT`
- `INSTRUCTIONS_LOADED`
- `WORKTREE_CREATE`
- `WORKTREE_REMOVE`
- `TEAMMATE_IDLE`

### Session Management

| Feature | Python SDK | Java Implementation | Status |
|---------|------------|---------------------|--------|
| Create session | Yes | Yes | ✅ |
| Message history | Yes | Yes | ✅ |
| Session metadata | Yes | Yes | ✅ |
| Session timeout | Yes | Yes | ✅ |
| Session fork | Yes | Not implemented | ❌ |
| Session resume | Yes | Not implemented | ❌ |
| Session snapshot | Yes | Not implemented | ❌ |

### Tool System

| Feature | Python SDK | Java Implementation | Status |
|---------|------------|---------------------|--------|
| Built-in tools | Read, Write, Edit, Bash, Glob, Grep | Same + Task tools | ✅ |
| Custom tools | `@tool` decorator | `@Tool` annotation | ✅ |
| MCP integration | `create_sdk_mcp_server()` | `MCPStdioTool`, `MCPStreamableHTTPTool` | ✅ |
| Tool schema generation | Automatic | Automatic | ✅ |

---

## Implementation Status Summary

### ✅ Implemented Features

1. Core Agent (Agent, LoopAgent)
2. Tool System (annotations, executor, MCP)
3. Hook System (20 events, matcher, priority)
4. Session Management (basic)
5. Most Agent Configuration Options

### ⚠️ Partial Features

1. **Session Management**: Fork and resume not implemented
2. **Agent Configuration**: Some options need integration

### ❌ Missing Features

1. **Session Fork/Resume** - Priority P3
2. **Custom Agents (AgentDefinition)** - Priority P3
3. **Partial Messages** - Priority P3

### N/A (Not Applicable)

1. **Sandbox** - Python SDK uses CLI with sandbox, Java uses native API
2. **Thinking/Effort** - Anthropic extended thinking (may be added later)
3. **File Checkpointing** - CLI-specific feature

---

## Roadmap

### Priority 1 (Quick Wins) - ✅ In Progress
- [x] Add disallowed_tools
- [x] Add max_budget_usd (CostTracker)
- [x] Add fallback_model
- [x] Add permission_mode

### Priority 2 (Core Features)
- [ ] Implement session fork/resume
- [ ] Implement permission mode enforcement

### Priority 3 (Enhancements)
- [ ] Implement custom agents
- [ ] Add partial message support
