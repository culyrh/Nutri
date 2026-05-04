package com.example.nutriuniv.domain.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class RegisterRequest {
    private String provider;
    private String oauthId;

    @NotBlank(message = "올바른 정보를 입력해 주세요.")
    @Size(min = 2, message = "올바른 정보를 입력해 주세요.")
    @Pattern(regexp = "^[가-힣a-zA-Z]+$", message = "올바른 정보를 입력해 주세요.")
    private String name;

    @NotBlank(message = "올바른 정보를 입력해 주세요.")
    @Email(
            message = "올바른 정보를 입력해 주세요.",
            regexp = "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
    )
    private String email;

    @NotBlank(message = "올바른 정보를 입력해 주세요.")
    @Pattern(regexp = "^(MALE|FEMALE)$", message = "올바른 정보를 입력해 주세요.")
    private String gender;

    @NotNull(message = "올바른 정보를 입력해 주세요.")
    @PastOrPresent(message = "올바른 정보를 입력해 주세요.")
    private LocalDate birthDate;
}