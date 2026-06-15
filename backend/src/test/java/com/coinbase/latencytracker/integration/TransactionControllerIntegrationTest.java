package com.coinbase.latencytracker.integration;

import com.coinbase.latencytracker.dto.TransactionRequest;
import com.coinbase.latencytracker.dto.TransactionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full Spring context integration tests.
 *
 * Uses H2 in-memory DB (application-test.yml) and a mocked Redis
 * so no Docker is required at this layer.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TransactionControllerIntegrationTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    // ─── Happy paths ────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @WithMockUser(username = "admin", roles = "USER")
    void createTransaction_validRequest_returns2xx() throws Exception {
        TransactionRequest req = validRequest();

        MvcResult result = mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.latencyReport.stages").isArray())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        TransactionResponse resp = objectMapper.readValue(body, TransactionResponse.class);

        assertThat(resp.getTransactionId()).isNotBlank();
        assertThat(resp.getLatencyReport().getStages()).hasSize(6);
        assertThat(resp.getConvertedAmount()).isPositive();
    }

    @Test
    @Order(2)
    void getHealth_publicEndpoint_returnsUp() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @Order(3)
    @WithMockUser
    void getAlerts_emptyDb_returnsEmptyPage() throws Exception {
        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(4)
    @WithMockUser
    void getStats_defaultWindow_returnsStats() throws Exception {
        mockMvc.perform(get("/api/v1/latency/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.window").value("1h"))
                .andExpect(jsonPath("$.stageStats").isArray());
    }

    // ─── Validation error cases ──────────────────────────────────────────────

    @Test
    @WithMockUser
    void createTransaction_blankUserId_returns400() throws Exception {
        TransactionRequest req = validRequest();
        req.setUserId("");

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("userId"));
    }

    @Test
    @WithMockUser
    void createTransaction_invalidCurrencyFormat_returns400() throws Exception {
        TransactionRequest req = validRequest();
        req.setFromCurrency("btc"); // must be uppercase

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("fromCurrency"));
    }

    @Test
    @WithMockUser
    void createTransaction_negativeAmount_returns400() throws Exception {
        TransactionRequest req = validRequest();
        req.setAmount(new BigDecimal("-10"));

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void createTransaction_nullAmount_returns400() throws Exception {
        TransactionRequest req = validRequest();
        req.setAmount(null);

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"));
    }

    @Test
    @WithMockUser
    void createTransaction_missingBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void createTransaction_amountExceedsMax_returns400() throws Exception {
        TransactionRequest req = validRequest();
        req.setAmount(new BigDecimal("99999999")); // over 10M limit

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ─── Security cases ──────────────────────────────────────────────────────

    @Test
    void createTransaction_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAlerts_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isUnauthorized());
    }

    // ─── Alert resolution ────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void resolveAlert_notFound_returns404() throws Exception {
        mockMvc.perform(patch("/api/v1/alerts/99999/resolve"))
                .andExpect(status().isNotFound());
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private TransactionRequest validRequest() {
        return TransactionRequest.builder()
                .userId("user-integration-test")
                .fromCurrency("BTC")
                .toCurrency("USD")
                .amount(new BigDecimal("0.5"))
                .build();
    }
}
