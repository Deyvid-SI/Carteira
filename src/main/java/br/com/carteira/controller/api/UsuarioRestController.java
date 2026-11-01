package br.com.carteira.controller.api;

import br.com.carteira.dto.AlterarSenhaRequest;
import br.com.carteira.dto.UsuarioCreateRequest;
import br.com.carteira.dto.UsuarioResponse;
import br.com.carteira.dto.UsuarioUpdateRequest;
import br.com.carteira.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Administração de contas. A anotação na classe vale para todos os métodos: somente ADMIN gerencia usuários.
 * Não há DELETE: a conta é desativada para preservar o histórico da carteira e permitir auditoria.
 */
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Usuários", description = "Gerenciamento de contas restrito ao papel ADMIN")
public class UsuarioRestController {
    private final UsuarioService service;

    public UsuarioRestController(UsuarioService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Listar usuários com paginação")
    public Page<UsuarioResponse> listar(Pageable pageable) {
        return service.listar(pageable).map(UsuarioResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por id")
    public UsuarioResponse buscar(@PathVariable Long id) { return UsuarioResponse.from(service.buscar(id)); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar usuário com papel definido pelo administrador")
    public UsuarioResponse criar(@Valid @RequestBody UsuarioCreateRequest request) {
        return UsuarioResponse.from(service.criar(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Alterar nome, e-mail, papel e situação (ativo/inativo)")
    public UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioUpdateRequest request) {
        return UsuarioResponse.from(service.atualizar(id, request));
    }

    @PutMapping("/{id}/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Definir uma nova senha")
    public void alterarSenha(@PathVariable Long id, @Valid @RequestBody AlterarSenhaRequest request) {
        service.alterarSenha(id, request);
    }
}
