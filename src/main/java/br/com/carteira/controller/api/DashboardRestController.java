package br.com.carteira.controller.api;

import br.com.carteira.dto.DashboardResponse;
import br.com.carteira.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard")
public class DashboardRestController {
    private final DashboardService service;

    public DashboardRestController(DashboardService service) { this.service = service; }

    @GetMapping("/dados")
    @Operation(summary = "Obter indicadores, séries para gráficos e tabela de ativos")
    public DashboardResponse dados() { return service.dados(); }
}
