package br.com.carteira.controller.web;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** O login é a única tela pública. Novas contas são criadas pelo administrador. */
@Controller
public class AuthController {

    @GetMapping("/")
    public String home(Principal principal) { return principal == null ? "redirect:/login" : "redirect:/dashboard"; }

    @GetMapping("/login")
    public String login() { return "login"; }
}
