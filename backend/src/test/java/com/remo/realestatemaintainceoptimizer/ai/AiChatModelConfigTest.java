package com.remo.realestatemaintainceoptimizer.ai;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Verifies that the Claude chat model bean exists only while the AI optimization is enabled and is configured with the configured model.
 */
class AiChatModelConfigTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(AiChatModelConfig.class);

    @Test
    void providesNoChatModelWhileTheAiIsDisabled() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(ChatModel.class));
    }

    @Test
    void providesAnAnthropicChatModelForTheConfiguredModelWhileTheAiIsEnabled() {
        contextRunner
                .withPropertyValues("remo.ai.enabled=true", "remo.ai.api-key=test-key", "remo.ai.model=claude-haiku-4-5")
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatModel.class);
                    ChatModel model = context.getBean(ChatModel.class);
                    assertThat(model).isInstanceOf(AnthropicChatModel.class);
                    assertThat(model.defaultRequestParameters().modelName()).isEqualTo("claude-haiku-4-5");
                    assertThat(model.defaultRequestParameters().maxOutputTokens()).isEqualTo(4096);
                });
    }

    @Test
    void failsAtStartupWhenTheAiIsEnabledWithoutAnApiKey() {
        contextRunner.withPropertyValues("remo.ai.enabled=true").run(context -> assertThat(context).hasFailed());
    }
}
