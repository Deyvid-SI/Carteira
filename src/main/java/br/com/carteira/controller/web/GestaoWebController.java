package br.com.carteira.controller.web;

import br.com.carteira.dto.CadastrarAcaoRequest;
import br.com.carteira.dto.AtualizarPosicaoRequest;
import br.com.carteira.dto.CadastrarCorretoraRequest;
import br.com.carteira.dto.RegistrarNegociacaoRequest;
import br.com.carteira.exception.RegraNegocioException;
import br.com.carteira.exception.RecursoNaoEncontradoException;
import br.com.carteira.exception.ServicoExternoException;
import br.com.carteira.model.Mercado;
import br.com.carteira.model.TipoNegociacao;
import br.com.carteira.service.AcaoService;
import br.com.carteira.service.CorretoraService;
import br.com.carteira.service.NegociacaoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;

@Controller
@RequestMapping("/gestao")
public class GestaoWebController {
    private final CorretoraService corretoraService;
    private final AcaoService acaoService;
    private final NegociacaoService negociacaoService;

    public GestaoWebController(CorretoraService corretoraService, AcaoService acaoService,
                               NegociacaoService negociacaoService) {
        this.corretoraService = corretoraService;
        this.acaoService = acaoService;
        this.negociacaoService = negociacaoService;
    }

    @GetMapping("/corretoras")
    public String corretoras(Model model) {
        if (!model.containsAttribute("corretoraForm")) model.addAttribute("corretoraForm", new CadastrarCorretoraRequest());
        model.addAttribute("corretoras", corretoraService.listarTodos());
        return "corretoras";
    }

    @PostMapping("/corretoras")
    public String cadastrarCorretora(@Valid @ModelAttribute("corretoraForm") CadastrarCorretoraRequest request,
                                     BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("corretoras", corretoraService.listarTodos());
            return "corretoras";
        }
        try {
            corretoraService.cadastrar(request);
            redirect.addFlashAttribute("sucesso", "Corretora validada e cadastrada com sucesso.");
            return "redirect:/gestao/corretoras";
        } catch (RegraNegocioException | ServicoExternoException ex) {
            binding.reject("corretora.invalida", ex.getMessage());
            model.addAttribute("corretoras", corretoraService.listarTodos());
            return "corretoras";
        }
    }

    @GetMapping("/acoes")
    public String acoes(Model model) {
        if (!model.containsAttribute("acaoForm")) model.addAttribute("acaoForm", new CadastrarAcaoRequest());
        model.addAttribute("acoes", acaoService.listarTodos());
        model.addAttribute("mercados", Mercado.values());
        return "acoes";
    }

    @PostMapping("/acoes")
    public String cadastrarAcao(@Valid @ModelAttribute("acaoForm") CadastrarAcaoRequest request,
                                BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) return acoesComErros(model);
        try {
            acaoService.cadastrar(request);
            redirect.addFlashAttribute("sucesso", "Ticker validado e cadastrado com sucesso.");
            return "redirect:/gestao/acoes";
        } catch (RegraNegocioException | ServicoExternoException ex) {
            binding.reject("acao.invalida", ex.getMessage());
            return acoesComErros(model);
        }
    }

    @PostMapping("/acoes/{id}/atualizar")
    public String atualizarCotacao(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            acaoService.atualizarCotacao(id);
            redirect.addFlashAttribute("sucesso", "Cotação atualizada e adicionada ao histórico.");
        } catch (RegraNegocioException | ServicoExternoException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/gestao/acoes";
    }

    @PostMapping("/acoes/{id}/posicao")
    public String atualizarPosicao(@PathVariable Long id,
                                   @RequestParam BigDecimal quantidade,
                                   @RequestParam BigDecimal precoMedio,
                                   RedirectAttributes redirect) {
        if (quantidade.signum() <= 0 || quantidade.stripTrailingZeros().scale() > 0 || precoMedio.signum() <= 0) {
            redirect.addFlashAttribute("erro", "A quantidade deve ser um número inteiro e o preço médio deve ser maior que zero.");
            return "redirect:/gestao/acoes";
        }
        AtualizarPosicaoRequest request = new AtualizarPosicaoRequest();
        request.setQuantidade(quantidade);
        request.setPrecoMedio(precoMedio);
        acaoService.atualizarPosicao(id, request);
        redirect.addFlashAttribute("sucesso", "Posição atualizada. Os gráficos foram recalculados.");
        return "redirect:/gestao/acoes";
    }

    @GetMapping("/negociacoes")
    public String negociacoes(@RequestParam(required = false) Long acaoId, Model model) {
        if (!model.containsAttribute("negociacaoForm")) {
            RegistrarNegociacaoRequest form = new RegistrarNegociacaoRequest();
            form.setAcaoId(acaoId);
            model.addAttribute("negociacaoForm", form);
        }
        return negociacoesComDados(model);
    }

    @PostMapping("/negociacoes")
    public String registrarNegociacao(
            @Valid @ModelAttribute("negociacaoForm") RegistrarNegociacaoRequest request,
            BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) return negociacoesComDados(model);
        try {
            var negociacao = negociacaoService.registrar(request);
            redirect.addFlashAttribute("sucesso", negociacao.getTipo().getDescricao() + " de "
                    + negociacao.getQuantidade().stripTrailingZeros().toPlainString() + " "
                    + negociacao.getAcao().getTicker() + " registrada. A posição foi recalculada.");
            return "redirect:/gestao/negociacoes";
        } catch (RegraNegocioException | RecursoNaoEncontradoException ex) {
            binding.reject("negociacao.invalida", ex.getMessage());
            return negociacoesComDados(model);
        }
    }

    private String acoesComErros(Model model) {
        model.addAttribute("acoes", acaoService.listarTodos());
        model.addAttribute("mercados", Mercado.values());
        return "acoes";
    }

    private String negociacoesComDados(Model model) {
        model.addAttribute("acoes", acaoService.listarTodos());
        model.addAttribute("tipos", TipoNegociacao.values());
        model.addAttribute("negociacoes", negociacaoService.listarRecentes());
        return "negociacoes";
    }
}
