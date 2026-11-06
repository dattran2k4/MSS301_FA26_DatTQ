package com.mss301.account.dto;
import lombok.*;
@Data @Builder public class LoginResponse { private String accessToken, refreshToken, tokenType, username; private Integer role; }
