# Migrating from AutoGen to Agent Framework (Java)

This guide helps developers migrate their applications from AutoGen (Python) to Agent Framework (Java).

## Core Concepts Mapping

| AutoGen Concept | Agent Framework (Java) Equivalent | Description |
| :--- | :--- | :--- |
| `ConversableAgent` | `Agent` (Interface) | The core interface for all agents. |
| `AssistantAgent` | `AgentBuilder` | Use `AgentBuilder` to create standard assistant agents. |
| `UserProxyAgent` | `AgentSession` / Manual Input | Instead of a proxy agent, use `AgentSession.run(Message.user(...))` to inject user input. |
| `GroupChat` | `GroupChatAgentBuilder` (Orchestration) | Use orchestration builders to manage multi-agent conversations. |
| `register_function` | `FunctionTool` | Use `FunctionTool` to wrap Java methods as tools. |
| `register_reply` | `AgentMiddleware` | Use Middleware to intercept and modify messages/responses. |

## Migration Patterns

### 1. Two-Agent Chat (User Proxy <-> Assistant)

**AutoGen (Python):**
```python
assistant = AssistantAgent("assistant", llm_config=...)
user_proxy = UserProxyAgent("user_proxy", human_input_mode="ALWAYS")
user_proxy.initiate_chat(assistant, message="Hello")
```

**Agent Framework (Java):**
```java
ChatClient client = OpenAIChatClient.builder()...build();
Agent assistant = AgentBuilder.builder()
    .name("assistant")
    .client(client)
    .build();

AgentSession session = assistant.createSession();
session.run(Message.user("Hello"));
```

### 2. Group Chat

**AutoGen (Python):**
```python
groupchat = GroupChat(agents=[user_proxy, engineer, scientist], messages=[], max_round=10)
manager = GroupChatManager(groupchat=groupchat, llm_config=...)
user_proxy.initiate_chat(manager, message="Research quantum physics")
```

**Agent Framework (Java):**
*Note: GroupChat orchestration is currently implemented via custom workflows or builders.*

```java
// Example using a custom GroupChat orchestration
List<Agent> agents = List.of(engineer, scientist);
ChatResponse result = new GroupChatOrchestrator(agents)
    .maxTurns(10)
    .run("Research quantum physics");
```

### 3. Tool Registration

**AutoGen (Python):**
```python
@user_proxy.register_for_execution()
@assistant.register_for_llm(description="Calculate sum")
def add(a: int, b: int) -> int:
    return a + b
```

**Agent Framework (Java):**
```java
public class MathTools {
    @Tool(description = "Calculate sum")
    public int add(int a, int b) {
        return a + b;
    }
}

// Register
agent.addTool(FunctionTool.create(
    MathTools.class.getMethod("add", int.class, int.class), 
    new MathTools()
));
```

## Key Differences

1.  **Strict Typing**: Java requires explicit types for tools and messages.
2.  **Builder Pattern**: Agent Framework heavily uses the Builder pattern for configuration.
3.  **Session Management**: State is explicitly managed via `AgentSession`, whereas AutoGen agents often hold their own state.
4.  **Middleware**: Agent Framework uses a middleware chain for extensibility, which is more flexible than AutoGen's hook system.
