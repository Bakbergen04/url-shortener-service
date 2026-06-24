package kg.jumabaev.shortener;

import org.springframework.boot.SpringApplication;

public class TestUrlShortenerApplication {

    public static void main(String[] args) {
        SpringApplication.from(UrlShortenerApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
