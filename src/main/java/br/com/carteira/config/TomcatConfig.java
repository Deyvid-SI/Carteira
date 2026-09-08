package br.com.carteira.config;

import org.apache.coyote.http11.Http11Nio2Protocol;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig {

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatProtocolCustomizer() {
        return factory -> factory.setProtocol(Http11Nio2Protocol.class.getName());
    }
}
