package br.com.carteira.model;

public enum AcaoAdmin {
    CRIOU_CONTA("Criou a conta"),
    ALTEROU_CADASTRO("Alterou o cadastro"),
    ALTEROU_PAPEL("Alterou o papel"),
    DESATIVOU_CONTA("Desativou a conta"),
    REATIVOU_CONTA("Reativou a conta"),
    REDEFINIU_SENHA("Redefiniu a senha");

    private final String descricao;

    AcaoAdmin(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
