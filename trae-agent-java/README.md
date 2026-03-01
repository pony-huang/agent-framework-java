# trae-agent-java

Java implementation of Trae Agent - an LLM-based software engineering agent.

## Features

- Multi-LLM Provider support (OpenAI, Anthropic)
- Rich tool ecosystem:
  - Bash execution
  - File editing (view/create/str_replace/insert)
  - JSON file editing
  - Sequential thinking
  - Task completion detection
- YAML configuration
- Trajectory recording

## Quick Start

```bash
# Build
./gradlew build

# Run a task
./gradlew run --args="Fix the bug in UserService.java"

# Interactive mode
./gradlew run --args="--interactive"
```

## Configuration

Create `trae-config.yaml` in your project directory:

```yaml
provider: anthropic
model: claude-sonnet-4-20250514
api_key: ${ANTHROPIC_API_KEY}

tools:
  - bash
  - edit
  - json_edit
  - sequential_thinking
  - task_done

max_steps: 100
working_directory: .
```

## Architecture

See [AGENTS.md](../AGENTS.md) in the parent directory for architecture details.

## Dependencies

- agent-framework-java: Core agent framework
- SnakeYAML: YAML configuration parsing
- OpenTelemetry: Observability