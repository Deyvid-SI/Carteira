package br.com.carteira.dto;

import java.util.List;

public record UsuarioAutenticadoResponse(String email, List<String> authorities) {
}
