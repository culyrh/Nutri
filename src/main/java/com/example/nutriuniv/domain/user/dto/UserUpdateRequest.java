package com.example.nutriuniv.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class UserUpdateRequest {
    private String name;
    private String gender;
    private LocalDate birthDate;
}