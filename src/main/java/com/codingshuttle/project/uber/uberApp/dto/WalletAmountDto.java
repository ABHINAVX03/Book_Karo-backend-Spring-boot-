package com.codingshuttle.project.uber.uberApp.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class WalletAmountDto {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Amount must be at least 1.00")
    @DecimalMax(value = "100000.00", message = "Amount must be at most 100000.00")
    private BigDecimal amount;

    public WalletAmountDto() {
    }

    public WalletAmountDto(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
