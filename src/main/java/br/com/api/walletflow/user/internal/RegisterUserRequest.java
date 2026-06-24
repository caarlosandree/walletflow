package br.com.api.walletflow.user.internal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Payload de cadastro de usuário. */
record RegisterUserRequest(
        @NotBlank String fullName,
        @NotBlank String document,
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotNull UserType type) {
}
