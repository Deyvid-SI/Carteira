package br.com.carteira.controller.web;

import br.com.carteira.dto.DashboardResponse;
import br.com.carteira.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class DashboardWebController {
    private final DashboardService service;

    public DashboardWebController(DashboardService service) { this.service = service; }

    @GetMapping("/dashboard")
    public String dashboard() { return "dashboard"; }

    /** Dados dos gráficos consumidos pelo dashboard.js com a sessão do portal; clientes REST usam /api/dashboard/dados. */
    @GetMapping("/dashboard/dados")
    @ResponseBody
    public DashboardResponse dados() { return service.dados(); }
}
