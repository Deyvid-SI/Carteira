package br.com.carteira.exception;

public class ServicoExternoException extends RuntimeException {
    private final String servico;

    public ServicoExternoException(String servico, String message) {
        super(message);
        this.servico = servico;
    }

    public ServicoExternoException(String servico, String message, Throwable cause) {
        super(message, cause);
        this.servico = servico;
    }

    public String getServico() { return servico; }
}
