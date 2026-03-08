package github.ponyhuang.agentframework.types.block;

public class TextBlock implements Block {
    private final String type = "text";
    private final String text;

    public TextBlock() {
        this.text = "";
    }

    public TextBlock(String text) {
        this.text = text;
    }

    @Override
    public String getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public static TextBlock of(String text) {
        return new TextBlock(text);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextBlock textBlock = (TextBlock) o;
        return type.equals(textBlock.type) && (text != null ? text.equals(textBlock.text) : textBlock.text == null);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (text != null ? text.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TextBlock{type='" + type + "', text='" + text + "'}";
    }
}
