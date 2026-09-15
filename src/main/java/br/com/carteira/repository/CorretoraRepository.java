package br.com.carteira.repository;

import br.com.carteira.model.Corretora;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorretoraRepository extends JpaRepository<Corretora, Long> {
    Optional<Corretora> findByIdAndUsuarioId(Long id, Long usuarioId);
    Optional<Corretora> findByCnpjAndUsuarioId(String cnpj, Long usuarioId);
    boolean existsByCnpjAndUsuarioId(String cnpj, Long usuarioId);
    Page<Corretora> findAllByUsuarioId(Long usuarioId, Pageable pageable);
    List<Corretora> findAllByUsuarioIdOrderByRazaoSocialAsc(Long usuarioId);
    long countByUsuarioId(Long usuarioId);
}
