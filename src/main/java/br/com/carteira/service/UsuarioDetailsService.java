package br.com.carteira.service;

import br.com.carteira.model.Usuario;
import br.com.carteira.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Carrega a conta pelo e-mail para o login por formulário (sessão web) e para o login da API (JWT).
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {
    private final UsuarioRepository repository;

    public UsuarioDetailsService(UsuarioRepository repository) { this.repository = repository; }

    @Override
    public UserDetails loadUserByUsername(String email) {
        Usuario usuario = repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        return User.withUsername(usuario.getEmail())
                .password(usuario.getSenha())
                .roles(usuario.getPapel().name())
                .disabled(!usuario.isAtivo())
                .build();
    }
}
