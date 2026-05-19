package com.codingshuttle.project.uber.uberApp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthTokensDto {
    private String accessToken;
    private String refreshToken;
    private UserDto user;
}
