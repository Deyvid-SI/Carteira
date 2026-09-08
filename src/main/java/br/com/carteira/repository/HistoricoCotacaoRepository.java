package br.com.carteira.repository;

import br.com.carteira.model.HistoricoCotacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoCotacaoRepository extends JpaRepository<HistoricoCotacao, Long> {
    List<HistoricoCotacao> findTop30ByAcaoIdOrderByDataHoraDesc(Long acaoId);
}
