package github.ponyhuang.agentframework.a2a.types;

import java.util.Map;

public interface Part {
    String getKind();

    Map<String, Object> getMetadata();
}
