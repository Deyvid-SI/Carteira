package br.com.carteira.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CadastrarCorretoraRequest {
    @NotBlank(message = "Informe o CNPJ")
    @Pattern(regexp = "^(\\d{14}|\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})$", message = "CNPJ inválido")
    private String cnpj;

    @NotBlank(message = "Informe o CEP")
    @Pattern(regexp = "^(\\d{8}|\\d{5}-\\d{3})$", message = "CEP inválido")
    private String cep;

    @NotBlank(message = "Informe o número")
    @Pattern(regexp = "^\\d+$", message = "O número deve conter somente algarismos")
    @Size(max = 20, message = "O número deve ter no máximo 20 caracteres")
    private String numero;

    @Size(max = 120, message = "O complemento deve ter no máximo 120 caracteres")
    private String complemento;

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }
}
