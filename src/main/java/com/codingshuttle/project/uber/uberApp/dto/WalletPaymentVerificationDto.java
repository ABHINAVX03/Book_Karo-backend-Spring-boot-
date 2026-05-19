package com.codingshuttle.project.uber.uberApp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletPaymentVerificationDto {
    @NotBlank(message = "Razorpay order id is required")
    @Pattern(regexp = "^order_[A-Za-z0-9]+$", message = "Razorpay order id is invalid")
    private String razorpayOrderId;

    @NotBlank(message = "Razorpay payment id is required")
    @Pattern(regexp = "^pay_[A-Za-z0-9]+$", message = "Razorpay payment id is invalid")
    private String razorpayPaymentId;

    @NotBlank(message = "Razorpay signature is required")
    @Pattern(regexp = "^[a-fA-F0-9]{64}$", message = "Razorpay signature is invalid")
    private String razorpaySignature;
}
