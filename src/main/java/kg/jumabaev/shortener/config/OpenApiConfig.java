package kg.jumabaev.shortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI shortenerOpenApi(AppProperties appProperties) {
        return new OpenAPI()
                .info(new Info()
                        .title("URL Shortener Service API")
                        .version("v1")
                        .description("REST API for creating short links, redirecting users, and viewing click analytics.")
                        .contact(new Contact().name("Bakbergen")))
                .servers(List.of(new Server()
                        .url(appProperties.baseUrl())
                        .description("Configured application server")));
    }
}
