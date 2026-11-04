package br.com.carteira.service;

import br.com.carteira.model.AcaoAdmin;
import br.com.carteira.model.AtividadeAdmin;
import br.com.carteira.repository.AtividadeAdminRepository;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registra quem alterou cada conta. O detalhe nunca deve conter senha, hash ou token. */
@Service
public class AtividadeAdminService {
    private final AtividadeAdminRepository repository;

    public AtividadeAdminService(AtividadeAdminRepository repository) { this.repository = repository; }

    @Transactional
    public void registrar(AcaoAdmin acao, String conta, String detalhe) {
        repository.save(new AtividadeAdmin(responsavelAtual(), acao, conta, detalhe));
    }

    @Transactional(readOnly = true)
    public List<AtividadeAdmin> recentes() {
        return repository.findTop20ByOrderByDataHoraDescIdDesc();
    }

    private static String responsavelAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "sistema" : authentication.getName();
    }
}
