package br.com.carteira.service;

import br.com.carteira.model.Usuario;
import br.com.carteira.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioAtualService {
    private final UsuarioRepository repository;

    public UsuarioAtualService(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Usuario obter() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("É necessário entrar para acessar a carteira.");
        }
        return repository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("A conta autenticada não foi encontrada."));
    }

    @Transactional(readOnly = true)
    public Long id() {
        return obter().getId();
    }
}
