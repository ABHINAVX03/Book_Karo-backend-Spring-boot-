package com.codingshuttle.project.uber.uberApp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletPaymentOrderDto {
    private String key;
    private String orderId;
    private Integer amount;
    private String currency;
    private String name;
}
