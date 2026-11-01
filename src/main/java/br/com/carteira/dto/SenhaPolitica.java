package br.com.carteira.dto;

/** Política única de senha usada no cadastro web e na administração de usuários. */
public final class SenhaPolitica {
    public static final String REGEX = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,72}$";
    public static final String MENSAGEM = "Use ao menos 8 caracteres, com maiúscula, minúscula e número";

    private SenhaPolitica() { }
}
