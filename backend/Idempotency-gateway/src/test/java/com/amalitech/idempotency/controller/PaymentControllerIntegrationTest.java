package com.amalitech.idempotency.controller;

import com.amalitech.idempotency.dto.PaymentRequest;
import com.amalitech.idempotency.dto.PaymentResponse;
import com.amalitech.idempotency.service.PaymentProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    // Stubbing the processor avoids the 2-second sleep in every test;
    // the in-flight race is covered separately in IdempotencyServiceConcurrencyTest.
    @MockitoBean
    PaymentProcessor processor;

    @BeforeEach
    void stubProcessor() {
        when(processor.process(any())).thenAnswer(invocation -> {
            PaymentRequest r = invocation.getArgument(0);
            return new PaymentResponse(
                    "Charged %s %s".formatted(r.amount().toPlainString(), r.currency()));
        });
    }

    @Test
    void firstRequest_processesAndReturns200WithChargedMessage() throws Exception {
        mvc.perform(post("/process-payment")
                        .header("Idempotency-Key", uniqueKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("100", "GHS")))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-Cache-Hit"))
                .andExpect(jsonPath("$.message").value("Charged 100 GHS"));
    }

    @Test
    void duplicateRequest_replaysSameResponseWithXCacheHitHeader() throws Exception {
        String key = uniqueKey();
        String reqBody = body("100", "GHS");

        mvc.perform(post("/process-payment")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-Cache-Hit"));

        mvc.perform(post("/process-payment")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache-Hit", "true"))
                .andExpect(jsonPath("$.message").value("Charged 100 GHS"));
    }

    @Test
    void duplicateRequest_invokesProcessorOnlyOnce() throws Exception {
        String key = uniqueKey();
        String reqBody = body("100", "GHS");

        mvc.perform(post("/process-payment")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reqBody));
        mvc.perform(post("/process-payment")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reqBody));

        verify(processor, times(1)).process(any());
    }

    @Test
    void sameKeyDifferentBody_returns409WithRequiredMessage() throws Exception {
        String key = uniqueKey();

        mvc.perform(post("/process-payment")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("100", "GHS")))
                .andExpect(status().isOk());

        mvc.perform(post("/process-payment")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("500", "GHS")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Idempotency key already used for a different request body."));
    }

    @Test
    void missingIdempotencyKeyHeader_returns400() throws Exception {
        mvc.perform(post("/process-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("100", "GHS")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blankIdempotencyKeyHeader_returns400() throws Exception {
        mvc.perform(post("/process-payment")
                        .header("Idempotency-Key", "   ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("100", "GHS")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidBody_negativeAmount_returns400() throws Exception {
        mvc.perform(post("/process-payment")
                        .header("Idempotency-Key", uniqueKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("-1", "GHS")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidBody_emptyCurrency_returns400() throws Exception {
        mvc.perform(post("/process-payment")
                        .header("Idempotency-Key", uniqueKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100,\"currency\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    private String uniqueKey() {
        // Unique per test invocation so the shared InMemoryIdempotencyStore
        // (a Spring singleton) doesn't leak state between tests.
        return "test-" + UUID.randomUUID();
    }

    private String body(String amount, String currency) throws Exception {
        return json.writeValueAsString(new PaymentRequest(new BigDecimal(amount), currency));
    }
}
