package br.com.carteira.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI carteiraOpenApi() {
        String scheme = "basicAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Carteira de Ações API")
                        .version("1.0.0")
                        .description("API REST para cadastro validado de corretoras e ações brasileiras e americanas.")
                        .contact(new Contact().name("Projeto acadêmico")))
                .addSecurityItem(new SecurityRequirement().addList(scheme))
                .components(new Components().addSecuritySchemes(scheme,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")));
    }
}
