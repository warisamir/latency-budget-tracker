package com.coinbase.latencytracker.controller;

import com.coinbase.latencytracker.dto.TransactionRequest;
import com.coinbase.latencytracker.dto.TransactionResponse;
import com.coinbase.latencytracker.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * POST /api/v1/transactions
     *
     * Processes a currency conversion transaction, tracing all 6 pipeline stages
     * and enforcing latency budgets. Returns the result plus a full latency report.
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody TransactionRequest request) {

        log.info("Transaction received: userId={} {} → {} amount={}",
                request.getUserId(), request.getFromCurrency(),
                request.getToCurrency(), request.getAmount());

        TransactionResponse response = transactionService.process(request);

        HttpStatus status = response.getLatencyReport().isWithinBudget()
                ? HttpStatus.CREATED
                : HttpStatus.OK; // 200 with budget warning

        return ResponseEntity.status(status).body(response);
    }
}
