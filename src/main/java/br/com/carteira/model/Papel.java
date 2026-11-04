package br.com.carteira.model;

public enum Papel {
    ADMIN("Administrador"),
    USER("Investidor");

    private final String descricao;

    Papel(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
