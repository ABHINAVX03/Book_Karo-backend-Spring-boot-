package com.codingshuttle.project.uber.uberApp.dto;

import java.math.BigDecimal;

public class WalletAmountDto {

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
