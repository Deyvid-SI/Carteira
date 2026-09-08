package br.com.carteira.controller.api;

import br.com.carteira.dto.CadastrarCorretoraRequest;
import br.com.carteira.dto.CorretoraResponse;
import br.com.carteira.service.CorretoraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/corretoras")
@Tag(name = "Corretoras", description = "Cadastro enriquecido por Receita Federal, CVM e ViaCEP")
public class CorretoraRestController {
    private final CorretoraService service;

    public CorretoraRestController(CorretoraService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar uma corretora validada")
    public CorretoraResponse cadastrar(@Valid @RequestBody CadastrarCorretoraRequest request) {
        return CorretoraResponse.from(service.cadastrar(request));
    }

    @GetMapping
    @Operation(summary = "Listar corretoras com paginação")
    public Page<CorretoraResponse> listar(Pageable pageable) {
        return service.listar(pageable).map(CorretoraResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar corretora por id")
    public CorretoraResponse buscar(@PathVariable Long id) {
        return CorretoraResponse.from(service.buscar(id));
    }

    @GetMapping("/cnpj/{cnpj}")
    @Operation(summary = "Buscar corretora por CNPJ")
    public CorretoraResponse buscarPorCnpj(@PathVariable String cnpj) {
        return CorretoraResponse.from(service.buscarPorCnpj(cnpj));
    }
}
