package co.edu.uniquindio.poo.sicares.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentación de la API en http://localhost:8080/swagger-ui.html (botón "Authorize" para el token). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sicaresOpenApi() {
        final String esquema = "bearerAuth";
        return new OpenAPI()
                .info(new ProcessHandle.Info()
                        .title("SICARES API")
                        .description("Sistema de Caracterización y Seguimiento Educativo")
                        .version("1.0"))
                .addSecurityItem(new SecurityRequirement().addList(esquema))
                .components(new Components().addSecuritySchemes(esquema,
                        new SecurityScheme()
                                .name(esquema)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}

