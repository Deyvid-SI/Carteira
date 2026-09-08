package br.com.carteira.repository;

import br.com.carteira.model.Acao;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface AcaoRepository extends JpaRepository<Acao, Long> {
    Optional<Acao> findByTickerIgnoreCase(String ticker);
    boolean existsByTickerIgnoreCase(String ticker);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Acao a where a.id = :id")
    Optional<Acao> buscarParaNegociacao(@Param("id") Long id);
}
