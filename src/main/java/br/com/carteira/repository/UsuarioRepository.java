package br.com.carteira.repository;

import br.com.carteira.model.Papel;
import br.com.carteira.model.Usuario;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<Usuario> findAllByOrderByNomeAscEmailAsc();
    long countByAtivoTrue();
    long countByPapelAndAtivoTrue(Papel papel);
    long countByDataCadastroGreaterThanEqual(LocalDateTime inicio);
}
