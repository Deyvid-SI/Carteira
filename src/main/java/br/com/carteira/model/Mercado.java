package br.com.carteira.model;

public enum Mercado {
    BRASIL("Brasil", "B3", "BRL"),
    ESTADOS_UNIDOS("Estados Unidos", "NYSE/Nasdaq", "USD");

    private final String descricao;
    private final String bolsa;
    private final String moedaPadrao;

    Mercado(String descricao, String bolsa, String moedaPadrao) {
        this.descricao = descricao;
        this.bolsa = bolsa;
        this.moedaPadrao = moedaPadrao;
    }

    public String getDescricao() { return descricao; }
    public String getBolsa() { return bolsa; }
    public String getMoedaPadrao() { return moedaPadrao; }
}
