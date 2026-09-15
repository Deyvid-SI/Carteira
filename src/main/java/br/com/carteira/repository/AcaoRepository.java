package br.com.carteira.repository;

import br.com.carteira.model.Acao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface AcaoRepository extends JpaRepository<Acao, Long> {
    Optional<Acao> findByIdAndUsuarioId(Long id, Long usuarioId);
    Optional<Acao> findByTickerIgnoreCaseAndUsuarioId(String ticker, Long usuarioId);
    boolean existsByTickerIgnoreCaseAndUsuarioId(String ticker, Long usuarioId);
    Page<Acao> findAllByUsuarioId(Long usuarioId, Pageable pageable);
    List<Acao> findAllByUsuarioIdOrderByTickerAsc(Long usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Acao a where a.id = :id and a.usuario.id = :usuarioId")
    Optional<Acao> buscarParaNegociacao(@Param("id") Long id, @Param("usuarioId") Long usuarioId);
}
