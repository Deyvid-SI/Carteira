package br.com.carteira.integration.gateway;

public interface CvmGateway {
    CvmDados validarCorretora(String cnpj);

    record CvmDados(boolean registrada, String nome, String status) {}
}
