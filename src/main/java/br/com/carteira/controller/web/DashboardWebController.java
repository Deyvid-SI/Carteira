package br.com.carteira.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardWebController {
    @GetMapping("/dashboard")
    public String dashboard() { return "dashboard"; }
}
