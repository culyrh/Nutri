package com.example.nutriuniv.domain.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class UserUpdateRequest {

    @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    private String name;

    @Pattern(
            regexp = "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$",
            message = "올바른 이메일 형식이 아닙니다."
    )
    private String email;

    @Size(max = 50, message = "닉네임은 50자 이하여야 합니다.")
    private String nickname;

    private String gender;

    private LocalDate birthDate;
}