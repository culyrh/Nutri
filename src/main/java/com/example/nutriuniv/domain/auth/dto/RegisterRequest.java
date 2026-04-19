package com.example.nutriuniv.domain.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class RegisterRequest {
    private String provider;
    private String oauthId;
    private String name;
    private String email;
    private String gender;
    private LocalDate birthDate;
}