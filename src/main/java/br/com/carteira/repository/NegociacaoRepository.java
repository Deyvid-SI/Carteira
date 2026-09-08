package br.com.carteira.repository;

import br.com.carteira.model.Negociacao;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NegociacaoRepository extends JpaRepository<Negociacao, Long> {
    @EntityGraph(attributePaths = "acao")
    Page<Negociacao> findAllByOrderByDataNegociacaoDescIdDesc(Pageable pageable);

    @EntityGraph(attributePaths = "acao")
    List<Negociacao> findTop200ByOrderByDataNegociacaoDescIdDesc();

    @EntityGraph(attributePaths = "acao")
    List<Negociacao> findAllByOrderByDataNegociacaoAscIdAsc();
}
