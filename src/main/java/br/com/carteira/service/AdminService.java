package br.com.carteira.service;

import br.com.carteira.dto.PainelAdmin;
import br.com.carteira.model.Papel;
import br.com.carteira.repository.AcaoRepository;
import br.com.carteira.repository.CorretoraRepository;
import br.com.carteira.repository.NegociacaoRepository;
import br.com.carteira.repository.UsuarioRepository;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UsuarioRepository usuarioRepository;
    private final AcaoRepository acaoRepository;
    private final CorretoraRepository corretoraRepository;
    private final NegociacaoRepository negociacaoRepository;
    private final boolean brapiComToken;
    private final boolean alphaVantageComChavePropria;
    private final boolean consoleH2Ativo;

    /** As chaves só viram indicadores aqui: o valor delas nunca sai deste construtor. */
    public AdminService(UsuarioRepository usuarioRepository, AcaoRepository acaoRepository,
                        CorretoraRepository corretoraRepository, NegociacaoRepository negociacaoRepository,
                        @Value("${integracoes.brapi.token:}") String brapiToken,
                        @Value("${integracoes.alpha-vantage.api-key:demo}") String alphaVantageChave,
                        @Value("${spring.h2.console.enabled:false}") boolean consoleH2Ativo) {
        this.usuarioRepository = usuarioRepository;
        this.acaoRepository = acaoRepository;
        this.corretoraRepository = corretoraRepository;
        this.negociacaoRepository = negociacaoRepository;
        this.brapiComToken = !brapiToken.isBlank();
        this.alphaVantageComChavePropria = !alphaVantageChave.isBlank() && !"demo".equalsIgnoreCase(alphaVantageChave);
        this.consoleH2Ativo = consoleH2Ativo;
    }

    @Transactional(readOnly = true)
    public PainelAdmin painel() {
        long totalContas = usuarioRepository.count();
        long contasAtivas = usuarioRepository.countByAtivoTrue();
        return new PainelAdmin(
                totalContas,
                contasAtivas,
                totalContas - contasAtivas,
                usuarioRepository.countByPapelAndAtivoTrue(Papel.ADMIN),
                usuarioRepository.countByDataCadastroGreaterThanEqual(LocalDateTime.now().minusDays(30)),
                acaoRepository.count(),
                corretoraRepository.count(),
                negociacaoRepository.count(),
                acaoRepository.buscarUltimaCotacao(),
                brapiComToken,
                alphaVantageComChavePropria,
                consoleH2Ativo);
    }
}
