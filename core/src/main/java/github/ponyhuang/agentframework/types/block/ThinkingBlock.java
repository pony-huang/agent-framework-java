package github.ponyhuang.agentframework.types.block;

public class ThinkingBlock implements Block {
    private final String type = "thinking";
    private final String thinking;
    private final String signature;

    public ThinkingBlock() {
        this.thinking = "";
        this.signature = "";
    }

    public ThinkingBlock(String thinking) {
        this.thinking = thinking;
        this.signature = "";
    }

    public ThinkingBlock(String thinking, String signature) {
        this.thinking = thinking;
        this.signature = signature;
    }

    @Override
    public String getType() {
        return type;
    }

    public String getThinking() {
        return thinking;
    }

    public String getSignature() {
        return signature;
    }

    public static ThinkingBlock of(String thinking) {
        return new ThinkingBlock(thinking);
    }

    public static ThinkingBlock of(String thinking, String signature) {
        return new ThinkingBlock(thinking, signature);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThinkingBlock that = (ThinkingBlock) o;
        return type.equals(that.type) &&
                (thinking != null ? thinking.equals(that.thinking) : that.thinking == null) &&
                (signature != null ? signature.equals(that.signature) : that.signature == null);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (thinking != null ? thinking.hashCode() : 0);
        result = 31 * result + (signature != null ? signature.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ThinkingBlock{type='" + type + "', thinking='" + thinking + "', signature='" + signature + "'}";
    }
}
