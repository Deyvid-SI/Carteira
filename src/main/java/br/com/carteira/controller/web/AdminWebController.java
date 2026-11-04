package br.com.carteira.controller.web;

import br.com.carteira.dto.AlterarSenhaRequest;
import br.com.carteira.dto.UsuarioAdminForm;
import br.com.carteira.exception.RecursoNaoEncontradoException;
import br.com.carteira.exception.RegraNegocioException;
import br.com.carteira.model.Papel;
import br.com.carteira.service.AdminService;
import br.com.carteira.service.AtividadeAdminService;
import br.com.carteira.service.UsuarioService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Locale;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Área exclusiva do ADMIN. Mostra totais e contas, mas nunca a carteira de outra pessoa. */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminWebController {
    private static final String REDIRECT_USUARIOS = "redirect:/admin/usuarios";

    private final AdminService adminService;
    private final UsuarioService usuarioService;
    private final AtividadeAdminService atividades;

    public AdminWebController(AdminService adminService, UsuarioService usuarioService,
                              AtividadeAdminService atividades) {
        this.adminService = adminService;
        this.usuarioService = usuarioService;
        this.atividades = atividades;
    }

    @GetMapping
    public String painel(Model model) {
        model.addAttribute("painel", adminService.painel());
        model.addAttribute("atividades", atividades.recentes());
        return "admin";
    }

    @GetMapping("/usuarios")
    public String usuarios(Model model, Principal principal) {
        if (!model.containsAttribute("usuarioForm")) model.addAttribute("usuarioForm", new UsuarioAdminForm());
        return usuariosComDados(model, principal);
    }

    @PostMapping("/usuarios")
    public String criar(@Valid @ModelAttribute("usuarioForm") UsuarioAdminForm form, BindingResult binding,
                        Model model, Principal principal, RedirectAttributes redirect) {
        if (binding.hasErrors()) return usuariosComDados(model, principal);
        try {
            var criada = usuarioService.criar(form.paraRequest());
            redirect.addFlashAttribute("sucesso", "Conta de " + criada.getEmail() + " criada.");
            return REDIRECT_USUARIOS;
        } catch (RegraNegocioException ex) {
            binding.reject("usuario.invalido", ex.getMessage());
            return usuariosComDados(model, principal);
        }
    }

    @PostMapping("/usuarios/{id}/papel")
    public String definirPapel(@PathVariable Long id, @RequestParam Papel papel, Principal principal,
                               RedirectAttributes redirect) {
        try {
            var conta = usuarioService.definirPapel(id, papel, principal.getName());
            redirect.addFlashAttribute("sucesso",
                    conta.getEmail() + " agora é " + papel.getDescricao().toLowerCase(Locale.ROOT) + ".");
        } catch (RegraNegocioException | RecursoNaoEncontradoException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        return REDIRECT_USUARIOS;
    }

    @PostMapping("/usuarios/{id}/situacao")
    public String definirSituacao(@PathVariable Long id, @RequestParam boolean ativo, Principal principal,
                                  RedirectAttributes redirect) {
        try {
            var conta = usuarioService.definirSituacao(id, ativo, principal.getName());
            redirect.addFlashAttribute("sucesso", "Conta de " + conta.getEmail() + (ativo ? " reativada." : " desativada."));
        } catch (RegraNegocioException | RecursoNaoEncontradoException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        return REDIRECT_USUARIOS;
    }

    @PostMapping("/usuarios/{id}/senha")
    public String alterarSenha(@PathVariable Long id, @Valid @ModelAttribute AlterarSenhaRequest request,
                               BindingResult binding, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            redirect.addFlashAttribute("erro", binding.getAllErrors().get(0).getDefaultMessage());
            return REDIRECT_USUARIOS;
        }
        try {
            var conta = usuarioService.alterarSenha(id, request);
            redirect.addFlashAttribute("sucesso", "Senha de " + conta.getEmail() + " redefinida.");
        } catch (RecursoNaoEncontradoException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        return REDIRECT_USUARIOS;
    }

    private String usuariosComDados(Model model, Principal principal) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        model.addAttribute("papeis", Papel.values());
        model.addAttribute("emailSessao", principal.getName());
        return "admin-usuarios";
    }
}
