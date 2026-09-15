package br.com.carteira.controller.web;

import br.com.carteira.dto.ResumoFiscalAno;
import br.com.carteira.repository.UsuarioRepository;
import br.com.carteira.service.RelatorioCarteiraService;
import java.security.Principal;
import java.time.Year;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PortalWebController {
    private static final String AVISO_FISCAL = "Esta tela organiza os dados registrados na carteira e não substitui "
            + "o informe da corretora, o cálculo de um contador ou a declaração oficial à Receita Federal.";

    private final RelatorioCarteiraService relatorioService;
    private final UsuarioRepository usuarioRepository;

    public PortalWebController(RelatorioCarteiraService relatorioService, UsuarioRepository usuarioRepository) {
        this.relatorioService = relatorioService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/configuracoes")
    public String configuracoes(Principal principal, Model model) {
        String email = principal.getName();
        model.addAttribute("emailSessao", email);
        usuarioRepository.findByEmailIgnoreCase(email).ifPresent(usuario -> model.addAttribute("usuario", usuario));
        return "configuracoes";
    }

    @GetMapping("/historico")
    public String historico(Model model) {
        var negociacoes = relatorioService.listarHistorico();
        model.addAttribute("negociacoes", negociacoes);
        model.addAttribute("resumoHistorico", relatorioService.resumirHistorico(negociacoes));
        return "historico";
    }

    @GetMapping("/imposto-renda")
    public String impostoRenda(@RequestParam(required = false) Integer ano, Model model) {
        var negociacoes = relatorioService.listarHistorico();
        List<Integer> anos = relatorioService.listarAnos(negociacoes);
        int anoSelecionado = escolherAno(ano, anos);
        var negociacoesAno = relatorioService.filtrarAno(negociacoes, anoSelecionado);
        ResumoFiscalAno resumoSelecionado = relatorioService.resumirAno(anoSelecionado, negociacoesAno);

        model.addAttribute("avisoFiscal", AVISO_FISCAL);
        model.addAttribute("anoSelecionado", anoSelecionado);
        model.addAttribute("anosDisponiveis", anos);
        model.addAttribute("resumosFiscais", relatorioService.resumirPorAno(negociacoes));
        model.addAttribute("resumoFiscal", resumoSelecionado);
        model.addAttribute("negociacoesAno", negociacoesAno);
        return "imposto-renda";
    }

    private int escolherAno(Integer solicitado, List<Integer> disponiveis) {
        if (solicitado != null && (disponiveis.isEmpty() || disponiveis.contains(solicitado))) return solicitado;
        return disponiveis.isEmpty() ? Year.now().getValue() : disponiveis.get(0);
    }
}
