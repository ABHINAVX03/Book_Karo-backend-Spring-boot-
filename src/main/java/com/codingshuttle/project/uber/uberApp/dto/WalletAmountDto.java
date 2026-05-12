package com.codingshuttle.project.uber.uberApp.dto;

public class WalletAmountDto {

    private Double amount;

    public WalletAmountDto() {
    }

    public WalletAmountDto(Double amount) {
        this.amount = amount;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}
