package br.com.carteira.controller.api;

import br.com.carteira.dto.NegociacaoResponse;
import br.com.carteira.dto.RegistrarNegociacaoRequest;
import br.com.carteira.service.NegociacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/negociacoes")
@Tag(name = "Negociações", description = "Compras, vendas e atualização automática da posição")
public class NegociacaoRestController {
    private final NegociacaoService service;

    public NegociacaoRestController(NegociacaoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar compra ou venda pelo preço informado")
    public NegociacaoResponse registrar(@Valid @RequestBody RegistrarNegociacaoRequest request) {
        return NegociacaoResponse.from(service.registrar(request));
    }

    @GetMapping
    @Operation(summary = "Listar negociações da mais recente para a mais antiga")
    public Page<NegociacaoResponse> listar(Pageable pageable) {
        return service.listar(pageable).map(NegociacaoResponse::from);
    }
}
