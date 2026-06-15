package com.coinbase.latencytracker.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransactionRequest {

    @NotBlank(message = "userId must not be blank")
    @Size(max = 64, message = "userId must be ≤ 64 characters")
    private String userId;

    @NotBlank(message = "fromCurrency must not be blank")
    @Pattern(regexp = "^[A-Z]{3,10}$", message = "fromCurrency must be uppercase letters only")
    private String fromCurrency;

    @NotBlank(message = "toCurrency must not be blank")
    @Pattern(regexp = "^[A-Z]{3,10}$", message = "toCurrency must be uppercase letters only")
    private String toCurrency;

    @NotNull(message = "amount must not be null")
    @DecimalMin(value = "0.00000001", message = "amount must be positive")
    @DecimalMax(value = "10000000", message = "amount must be ≤ 10,000,000")
    @Digits(integer = 10, fraction = 8, message = "amount invalid precision")
    private BigDecimal amount;

    @Size(max = 200, message = "notes must be ≤ 200 characters")
    private String notes;
}
