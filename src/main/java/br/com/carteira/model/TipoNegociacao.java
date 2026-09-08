package br.com.carteira.model;

public enum TipoNegociacao {
    COMPRA("Compra"),
    VENDA("Venda");

    private final String descricao;

    TipoNegociacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
