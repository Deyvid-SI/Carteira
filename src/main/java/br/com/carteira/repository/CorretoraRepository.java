package br.com.carteira.repository;

import br.com.carteira.model.Corretora;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorretoraRepository extends JpaRepository<Corretora, Long> {
    Optional<Corretora> findByCnpj(String cnpj);
    boolean existsByCnpj(String cnpj);
}
