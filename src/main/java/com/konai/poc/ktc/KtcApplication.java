package com.konai.poc.ktc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication @EnableScheduling
public class KtcApplication {

    public static void main(String[] args) {
        SpringApplication.run(KtcApplication.class, args);
    }

}
