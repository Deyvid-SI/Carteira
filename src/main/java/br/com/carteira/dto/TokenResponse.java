package br.com.carteira.dto;

public record TokenResponse(String token, String tipo, long expiresIn) {
}
