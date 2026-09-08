package br.com.carteira.controller.web;

import br.com.carteira.dto.CadastroUsuarioRequest;
import br.com.carteira.exception.RegraNegocioException;
import br.com.carteira.service.UsuarioService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {
    private final UsuarioService service;

    public AuthController(UsuarioService service) { this.service = service; }

    @GetMapping("/")
    public String home(Principal principal) { return principal == null ? "redirect:/login" : "redirect:/dashboard"; }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/cadastro")
    public String cadastro(Model model) {
        if (!model.containsAttribute("usuario")) model.addAttribute("usuario", new CadastroUsuarioRequest());
        return "cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrar(@Valid @ModelAttribute("usuario") CadastroUsuarioRequest request,
                            BindingResult binding) {
        if (!request.getSenha().equals(request.getConfirmarSenha())) {
            binding.rejectValue("confirmarSenha", "senhas.diferentes", "As senhas não conferem");
        }
        if (binding.hasErrors()) return "cadastro";
        try {
            service.cadastrar(request);
            return "redirect:/login?cadastrado";
        } catch (RegraNegocioException ex) {
            binding.reject("cadastro.invalido", ex.getMessage());
            return "cadastro";
        }
    }
}
