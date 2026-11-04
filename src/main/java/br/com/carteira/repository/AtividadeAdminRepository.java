package br.com.carteira.repository;

import br.com.carteira.model.AtividadeAdmin;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtividadeAdminRepository extends JpaRepository<AtividadeAdmin, Long> {
    List<AtividadeAdmin> findTop20ByOrderByDataHoraDescIdDesc();
}
