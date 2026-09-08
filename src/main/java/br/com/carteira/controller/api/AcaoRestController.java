package br.com.carteira.controller.api;

import br.com.carteira.dto.AcaoResponse;
import br.com.carteira.dto.CadastrarAcaoRequest;
import br.com.carteira.dto.AtualizarPosicaoRequest;
import br.com.carteira.service.AcaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/acoes")
@Tag(name = "Ações", description = "Ativos brasileiros via brapi e americanos via Alpha Vantage")
public class AcaoRestController {
    private final AcaoService service;

    public AcaoRestController(AcaoService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar uma ação após validar o ticker")
    public AcaoResponse cadastrar(@Valid @RequestBody CadastrarAcaoRequest request) {
        return AcaoResponse.from(service.cadastrar(request));
    }

    @GetMapping
    @Operation(summary = "Listar ações com paginação")
    public Page<AcaoResponse> listar(Pageable pageable) {
        return service.listar(pageable).map(AcaoResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar ação por id")
    public AcaoResponse buscar(@PathVariable Long id) { return AcaoResponse.from(service.buscar(id)); }

    @GetMapping("/ticker/{ticker}")
    @Operation(summary = "Buscar ação por ticker")
    public AcaoResponse buscarPorTicker(@PathVariable String ticker) {
        return AcaoResponse.from(service.buscarPorTicker(ticker));
    }

    @PutMapping("/{id}/atualizar-cotacao")
    @Operation(summary = "Atualizar a cotação na API externa")
    public AcaoResponse atualizar(@PathVariable Long id) {
        return AcaoResponse.from(service.atualizarCotacao(id));
    }

    @PutMapping("/{id}/posicao")
    @Operation(summary = "Atualizar quantidade e preço médio da posição")
    public AcaoResponse atualizarPosicao(@PathVariable Long id, @Valid @RequestBody AtualizarPosicaoRequest request) {
        return AcaoResponse.from(service.atualizarPosicao(id, request));
    }
}
