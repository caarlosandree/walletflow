package br.com.api.walletflow.user.internal;

/** Resposta do cadastro (sem senha). */
record UserResponse(Long id, String fullName, String document, String email, UserType type) {
}
