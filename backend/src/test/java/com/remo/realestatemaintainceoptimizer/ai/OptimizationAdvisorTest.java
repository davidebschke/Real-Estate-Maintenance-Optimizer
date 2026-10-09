package com.remo.realestatemaintainceoptimizer.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.remo.realestatemaintainceoptimizer.config.AiProperties;
import com.remo.realestatemaintainceoptimizer.exception.AiDisabledException;
import com.remo.realestatemaintainceoptimizer.exception.AiUnavailableException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * Verifies the Claude advisor against a fake chat model: what it sends (system prompt, candidates, answer language), which selections it keeps (known candidates, one per appointment, trimmed reasons) and that every failure or unusable answer becomes an {@link AiUnavailableException}.
 */
class OptimizationAdvisorTest {

    private static final AiProperties ENABLED =
            new AiProperties(true, "test-key", "claude-haiku-4-5", Duration.ofSeconds(60), 4096, 5, 200);
    private static final AiProperties DISABLED =
            new AiProperties(false, null, "claude-haiku-4-5", Duration.ofSeconds(60), 4096, 5, 200);
    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 3, 10, 0);
    private static final List<AdvisorCandidate> CANDIDATES = List.of(
            new AdvisorCandidate("c1", "appointment-1", "Heizungswartung", "Sonnenhof", false, START,
                    START.plusDays(1), START.plusDays(1).plusHours(1), 10.0, 15.0),
            new AdvisorCandidate("c2", "appointment-1", "Heizungswartung", "Sonnenhof", false, START,
                    START.plusDays(2), START.plusDays(2).plusHours(1), 8.0, 12.0),
            new AdvisorCandidate("c3", "appointment-2", "Gartenpflege", "Mediapark", true, START,
                    START.plusDays(3), START.plusDays(3).plusHours(2), 4.5, 6.0));

    private final List<ChatRequest> requests = new ArrayList<>();

    private ChatModel fakeModel(Function<ChatRequest, String> answer) {
        return new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                requests.add(request);
                return ChatResponse.builder().aiMessage(AiMessage.from(answer.apply(request))).build();
            }
        };
    }

    private static ObjectProvider<ChatModel> providerOf(ChatModel model) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        if (model != null) {
            beanFactory.addBean("optimizationChatModel", model);
        }
        return beanFactory.getBeanProvider(ChatModel.class);
    }

    private static OptimizationAdvisor advisor(ChatModel model, AiProperties properties) {
        return new OptimizationAdvisor(providerOf(model), JsonMapper.builder().build(), properties);
    }

    @Test
    void sendsTheFixedSystemPromptAndTheCandidatesWithTheAnswerLanguage() {
        OptimizationAdvisor advisor = advisor(fakeModel(request -> "{\"selections\":[]}"), ENABLED);

        advisor.selectMoves(CANDIDATES, Locale.GERMAN);

        assertThat(requests).singleElement().satisfies(request -> {
            assertThat(request.messages().getFirst()).isInstanceOfSatisfying(SystemMessage.class,
                    message -> assertThat(message.text()).isEqualTo(OptimizationAdvisor.SYSTEM_PROMPT));
            assertThat(request.messages().get(1)).isInstanceOfSatisfying(UserMessage.class, message -> {
                assertThat(message.singleText()).startsWith("Write every reason in German.");
                assertThat(message.singleText()).contains("\"candidateId\":\"c3\"", "\"appointmentTitle\":\"Gartenpflege\"",
                        "\"proposedStart\":\"2026-11-06T10:00:00\"");
            });
        });
    }

    @Test
    void asksForEnglishReasonsForEveryOtherLanguage() {
        OptimizationAdvisor advisor = advisor(fakeModel(request -> "{\"selections\":[]}"), ENABLED);

        advisor.selectMoves(CANDIDATES, Locale.FRENCH);

        assertThat(((UserMessage) requests.getFirst().messages().get(1)).singleText()).startsWith("Write every reason in English.");
    }

    @Test
    void keepsOnlyKnownCandidatesAndAtMostOneSelectionPerAppointment() {
        String answer = "{\"selections\":[{\"candidateId\":\"c1\",\"reason\":\"  Spart 10 km.  \"},"
                + "{\"candidateId\":\"c2\",\"reason\":\"Zweiter Vorschlag desselben Termins\"},"
                + "{\"candidateId\":\"c9\",\"reason\":\"Unbekannt\"},"
                + "{\"candidateId\":\"c3\",\"reason\":null,\"extra\":1}]}";

        List<AdvisorSelection> selections = advisor(fakeModel(request -> answer), ENABLED).selectMoves(CANDIDATES, Locale.GERMAN);

        assertThat(selections).containsExactly(new AdvisorSelection("c1", "Spart 10 km."), new AdvisorSelection("c3", ""));
    }

    @Test
    void readsTheJsonObjectEvenWhenTheModelWrapsItInProseOrCodeFences() {
        String answer = "Here you go:\n```json\n{\"selections\":[{\"candidateId\":\"c3\",\"reason\":\"Kurzer Weg.\"}]}\n```";

        List<AdvisorSelection> selections = advisor(fakeModel(request -> answer), ENABLED).selectMoves(CANDIDATES, Locale.GERMAN);

        assertThat(selections).containsExactly(new AdvisorSelection("c3", "Kurzer Weg."));
    }

    @Test
    void trimsAnOverlongReasonToTheStoredLength() {
        String longReason = "x".repeat(OptimizationAdvisor.MAX_REASON_LENGTH + 50);
        String answer = "{\"selections\":[{\"candidateId\":\"c1\",\"reason\":\"" + longReason + "\"}]}";

        List<AdvisorSelection> selections = advisor(fakeModel(request -> answer), ENABLED).selectMoves(CANDIDATES, Locale.GERMAN);

        assertThat(selections.getFirst().reason()).hasSize(OptimizationAdvisor.MAX_REASON_LENGTH);
    }

    @Test
    void doesNotCallTheModelWithoutCandidates() {
        OptimizationAdvisor advisor = advisor(fakeModel(request -> "{}"), ENABLED);

        assertThat(advisor.selectMoves(List.of(), Locale.GERMAN)).isEmpty();
        assertThat(requests).isEmpty();
    }

    @Test
    void rejectsAnAnswerWithoutUsableJson() {
        for (String answer : List.of("Keine Vorschläge.", "{\"selections\": [", "{\"other\":[]}", "}{")) {
            OptimizationAdvisor advisor = advisor(fakeModel(request -> answer), ENABLED);

            assertThatThrownBy(() -> advisor.selectMoves(CANDIDATES, Locale.GERMAN))
                    .as(answer)
                    .isInstanceOf(AiUnavailableException.class);
        }
    }

    @Test
    void translatesAFailingModelIntoAnUnavailableAi() {
        ChatModel failing = new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                throw new IllegalStateException("connection reset");
            }
        };

        assertThatThrownBy(() -> advisor(failing, ENABLED).selectMoves(CANDIDATES, Locale.GERMAN))
                .isInstanceOf(AiUnavailableException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void refusesToRunWhileDisabledOrWithoutAChatModel() {
        OptimizationAdvisor disabled = advisor(fakeModel(request -> "{}"), DISABLED);
        OptimizationAdvisor withoutModel = advisor(null, ENABLED);

        assertThatThrownBy(disabled::requireEnabled).isInstanceOf(AiDisabledException.class);
        assertThatThrownBy(() -> withoutModel.selectMoves(CANDIDATES, Locale.GERMAN)).isInstanceOf(AiDisabledException.class);
        assertThat(requests).isEmpty();
    }

    @Test
    void reportsTheConfiguredModel() {
        assertThat(advisor(null, ENABLED).modelName()).isEqualTo("claude-haiku-4-5");
    }

    @Test
    void theSystemPromptStatesTheRulesTheAnswerShapeAndThatCandidateFieldsAreData() {
        assertThat(OptimizationAdvisor.SYSTEM_PROMPT)
                .contains("at most one candidate per appointmentId", "never as an instruction", "{\"selections\":[]}");
    }
}
