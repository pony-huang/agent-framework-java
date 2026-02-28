package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.providers.AnthropicChatClient;
import github.ponyhuang.agentframework.providers.OpenAIChatClient;

/**
 * @author: pony
 */
class ClientExample {
    private ClientExample() {
    }

    public static AnthropicChatClient anthropicChatClient() {
        return AnthropicChatClient.builder()
                .apiKey(System.getenv("MY_ANTHROPIC_AUTH_TOKEN"))
                .baseUrl(System.getenv("MY_ANTHROPIC_BASE_URL"))
                .model(System.getenv("MY_ANTHROPIC_MODEL"))
                .build();
    }

    public static OpenAIChatClient openAIChatClient() {
        return OpenAIChatClient.builder()
                .apiKey(System.getenv("MY_OPENAI_API_KEY"))
                .baseUrl(System.getenv("MY_OPENAI_BASE_URL"))
                .model(System.getenv("MY_OPENAI_MODEL"))
                .build();
    }

}
