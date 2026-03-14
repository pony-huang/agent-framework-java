package github.ponyhuang.agentframework.types.block;

import java.util.Map;
import java.util.Objects;

public class ToolUseBlock implements Block {
    private final String type = "tool_use";
    private final String id;
    private final String name;
    private final Map<String, Object> input;

    public ToolUseBlock() {
        this.id = "";
        this.name = "";
        this.input = null;
    }

    public ToolUseBlock(String id, String name, Map<String, Object> input) {
        this.id = id;
        this.name = name;
        this.input = input;
    }

    @Override
    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getInput() {
        return input;
    }

    public static ToolUseBlock of(String id, String name, Map<String, Object> input) {
        return new ToolUseBlock(id, name, input);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ToolUseBlock that = (ToolUseBlock) o;
        return (Objects.equals(id, that.id)) && (Objects.equals(name, that.name)) && (Objects.equals(input, that.input));
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (input != null ? input.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ToolUseBlock{type='" + type + "', id='" + id + "', name='" + name + "', input=" + input + "}";
    }
}
