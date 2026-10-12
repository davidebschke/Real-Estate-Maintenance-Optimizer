package com.remo.realestatemaintainceoptimizer.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.remo.realestatemaintainceoptimizer.ai.RentPaymentAdvisor.ExtractedPayment;
import com.remo.realestatemaintainceoptimizer.config.AiProperties;
import com.remo.realestatemaintainceoptimizer.config.PaymentProperties;
import com.remo.realestatemaintainceoptimizer.exception.AiDisabledException;
import com.remo.realestatemaintainceoptimizer.exception.AiUnavailableException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.PdfFileContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.util.unit.DataSize;
import tools.jackson.databind.json.JsonMapper;

/**
 * Verifies the Claude statement reader against a fake chat model: what it sends (system prompt, the PDF as base64, the scan model and token limit), which rows it keeps (plausible dates, amounts and periods, bounded texts and count) and that every failure or unusable answer becomes an {@link AiUnavailableException}.
 */
class RentPaymentAdvisorTest {

    private static final AiProperties ENABLED =
            new AiProperties(true, "test-key", "claude-haiku-4-5", Duration.ofSeconds(60), 4096, 5, 200);
    private static final AiProperties DISABLED =
            new AiProperties(false, null, "claude-haiku-4-5", Duration.ofSeconds(60), 4096, 5, 200);
    private static final PaymentProperties PAYMENTS = new PaymentProperties(
            5, "claude-haiku-5-5", 8192, DataSize.ofMegabytes(5), 10, 200, ZoneId.of("Europe/Berlin"));
    private static final byte[] PDF = "%PDF-1.7 statement".getBytes(StandardCharsets.US_ASCII);

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

    private static RentPaymentAdvisor advisor(ChatModel model, AiProperties properties) {
        return new RentPaymentAdvisor(providerOf(model), JsonMapper.builder().build(), properties, PAYMENTS);
    }

    private RentPaymentAdvisor advisorAnswering(String answer) {
        return advisor(fakeModel(request -> answer), ENABLED);
    }

    @Test
    void sendsTheSystemPromptAndThePdfAsBase64WithTheScanModelAndTokenLimit() {
        advisorAnswering("{\"payments\":[]}").extractPayments(PDF);

        assertThat(requests).singleElement().satisfies(request -> {
            assertThat(request.modelName()).isEqualTo("claude-haiku-5-5");
            assertThat(request.maxOutputTokens()).isEqualTo(8192);
            assertThat(request.messages().getFirst()).isInstanceOfSatisfying(SystemMessage.class,
                    message -> assertThat(message.text()).isEqualTo(RentPaymentAdvisor.SYSTEM_PROMPT));
            assertThat(request.messages().get(1)).isInstanceOfSatisfying(UserMessage.class, message ->
                    assertThat(message.contents()).anySatisfy(content -> assertThat(content).isInstanceOfSatisfying(
                            PdfFileContent.class, pdf -> {
                                assertThat(pdf.pdfFile().base64Data()).isEqualTo(Base64.getEncoder().encodeToString(PDF));
                                assertThat(pdf.pdfFile().mimeType()).isEqualTo("application/pdf");
                            })));
        });
    }

    @Test
    void returnsTheValidatedPaymentsOfTheAnswer() {
        List<ExtractedPayment> payments = advisorAnswering("""
                {"payments":[{"paymentDate":"2026-10-02","amount":950.5,"payerName":"  Max Mustermann ",
                "reference":"Miete Oktober 2026","periodYear":2026,"periodMonth":10}]}""").extractPayments(PDF);

        assertThat(payments).singleElement().satisfies(payment -> {
            assertThat(payment.paymentDate()).isEqualTo(LocalDate.of(2026, 10, 2));
            assertThat(payment.amount()).isEqualByComparingTo("950.50");
            assertThat(payment.payerName()).isEqualTo("Max Mustermann");
            assertThat(payment.reference()).isEqualTo("Miete Oktober 2026");
            assertThat(payment.periodYear()).isEqualTo(2026);
            assertThat(payment.periodMonth()).isEqualTo(10);
        });
    }

    @Test
    void defaultsTheRentPeriodToTheMonthOfThePaymentDate() {
        List<ExtractedPayment> payments = advisorAnswering("""
                {"payments":[
                {"paymentDate":"2026-10-30","amount":800,"payerName":"A","reference":"Miete"},
                {"paymentDate":"2026-09-28","amount":800,"payerName":"B","reference":"Miete","periodYear":1999,"periodMonth":9},
                {"paymentDate":"2026-08-03","amount":800,"payerName":"C","reference":"Miete","periodYear":2026,"periodMonth":13}]}""")
                .extractPayments(PDF);

        assertThat(payments).extracting(ExtractedPayment::periodYear, ExtractedPayment::periodMonth)
                .containsExactly(tuple(2026, 10),
                        tuple(2026, 9),
                        tuple(2026, 8));
    }

    @Test
    void dropsRowsWithoutAPlausibleDateOrAmount() {
        List<ExtractedPayment> payments = advisorAnswering("""
                {"payments":[
                {"paymentDate":"not a date","amount":800,"payerName":"A","reference":"x"},
                {"paymentDate":"1970-01-01","amount":800,"payerName":"A","reference":"x"},
                {"paymentDate":null,"amount":800,"payerName":"A","reference":"x"},
                {"paymentDate":"2026-10-02","amount":-5,"payerName":"A","reference":"x"},
                {"paymentDate":"2026-10-02","amount":0,"payerName":"A","reference":"x"},
                {"paymentDate":"2026-10-02","amount":99999999,"payerName":"A","reference":"x"},
                {"paymentDate":"2026-10-02","payerName":"A","reference":"x"},
                null,
                {"paymentDate":"2026-10-02","amount":800,"payerName":"Keeper","reference":"x"}]}""").extractPayments(PDF);

        assertThat(payments).extracting(ExtractedPayment::payerName).containsExactly("Keeper");
    }

    @Test
    void boundsTheLengthOfTextsAndTreatsMissingTextsAsEmpty() {
        String longText = "x".repeat(400);
        List<ExtractedPayment> payments = advisorAnswering("{\"payments\":[{\"paymentDate\":\"2026-10-02\",\"amount\":800,"
                + "\"payerName\":\"" + longText + "\",\"reference\":\"" + longText + "\"},"
                + "{\"paymentDate\":\"2026-10-03\",\"amount\":800}]}").extractPayments(PDF);

        assertThat(payments.getFirst().payerName()).hasSize(RentPaymentAdvisor.MAX_PAYER_NAME_LENGTH);
        assertThat(payments.getFirst().reference()).hasSize(200);
        assertThat(payments.get(1).payerName()).isEmpty();
        assertThat(payments.get(1).reference()).isEmpty();
    }

    @Test
    void keepsAtMostTheMaximumNumberOfPayments() {
        String row = "{\"paymentDate\":\"2026-10-02\",\"amount\":800,\"payerName\":\"A\",\"reference\":\"x\"}";
        String answer = "{\"payments\":[" + String.join(",", Collections.nCopies(RentPaymentAdvisor.MAX_PAYMENTS + 20, row)) + "]}";

        assertThat(advisorAnswering(answer).extractPayments(PDF)).hasSize(RentPaymentAdvisor.MAX_PAYMENTS);
    }

    @Test
    void toleratesProseAndCodeFencesAroundTheJson() {
        List<ExtractedPayment> payments = advisorAnswering("""
                Here you go:
                ```json
                {"payments":[{"paymentDate":"2026-10-02","amount":800,"payerName":"A","reference":"x","unknown":1}]}
                ```""").extractPayments(PDF);

        assertThat(payments).hasSize(1);
    }

    @Test
    void anEmptyListIsAValidAnswer() {
        assertThat(advisorAnswering("{\"payments\":[]}").extractPayments(PDF)).isEmpty();
    }

    @Test
    void everyUnusableAnswerBecomesAnAiUnavailableException() {
        for (String answer : List.of("no json here", "{\"payments\": \"oops\"}", "{\"other\":[]}", "{broken json}", "")) {
            assertThatThrownBy(() -> advisorAnswering(answer).extractPayments(PDF))
                    .as(answer).isInstanceOf(AiUnavailableException.class);
        }
    }

    @Test
    void aFailingModelBecomesAnAiUnavailableExceptionWithTheCause() {
        ChatModel failing = new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                throw new IllegalStateException("timeout");
            }
        };

        assertThatThrownBy(() -> advisor(failing, ENABLED).extractPayments(PDF))
                .isInstanceOf(AiUnavailableException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void aSwitchedOffOrMissingChatModelIsReportedAsDisabled() {
        assertThatThrownBy(() -> advisor(fakeModel(request -> "{}"), DISABLED).requireEnabled())
                .isInstanceOf(AiDisabledException.class);
        assertThatThrownBy(() -> advisor(null, ENABLED).extractPayments(PDF)).isInstanceOf(AiDisabledException.class);
        assertThat(requests).isEmpty();
    }

    @Test
    void amountsKeepTwoDecimals() {
        List<ExtractedPayment> payments = advisorAnswering(
                "{\"payments\":[{\"paymentDate\":\"2026-10-02\",\"amount\":800.005,\"payerName\":\"A\",\"reference\":\"x\"}]}")
                .extractPayments(PDF);

        assertThat(payments.getFirst().amount()).isEqualTo(new BigDecimal("800.01"));
    }
}
