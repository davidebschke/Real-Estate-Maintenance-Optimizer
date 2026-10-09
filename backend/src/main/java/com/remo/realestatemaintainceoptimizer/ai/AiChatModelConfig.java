package com.remo.realestatemaintainceoptimizer.ai;

import com.remo.realestatemaintainceoptimizer.config.AiProperties;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the LangChain4j chat model connected to Claude via the Anthropic API, only while {@code remo.ai.enabled} is true so a server without an API key starts without it.
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiChatModelConfig {

    static final int MAX_RETRIES = 1;

    @Bean
    @ConditionalOnProperty(prefix = "remo.ai", name = "enabled", havingValue = "true")
    public ChatModel optimizationChatModel(AiProperties properties) {
        return AnthropicChatModel.builder()
                .apiKey(properties.apiKey())
                .modelName(properties.model())
                .maxTokens(properties.maxTokens())
                .timeout(properties.timeout())
                .maxRetries(MAX_RETRIES)
                .build();
    }
}
