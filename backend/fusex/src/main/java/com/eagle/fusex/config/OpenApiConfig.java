package com.eagle.fusex.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "basicAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("FUSEX - API de Gestão de Encaminhamentos e Pré-Guias")
                        .description("Backend RESTful Spring Boot para gestão de atendimentos do Sistema FUSEX (Fundo de Saúde do Exército).")
                        .version("1.0.0")
                        .contact(new Contact().name("Equipe Eagle FUSEX"))
                        .license(new License().name("Proprietário").url("https://fusex.eb.mil.br")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")
                                        .description("Autenticação HTTP Basic (credenciais de usuário e senha)")));
    }
}
