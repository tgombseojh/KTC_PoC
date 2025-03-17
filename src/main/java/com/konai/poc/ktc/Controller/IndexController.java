package com.konai.poc.ktc.Controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class IndexController {

    @GetMapping("/")
    public Mono<String> home() {
        return Mono.just("Hello World");
    }

    @GetMapping(value = "/ht", produces = MediaType.TEXT_HTML_VALUE)
    public Mono<ResponseEntity<ClassPathResource>> index() {
        // 만약 index.html 파일이 src/main/resources/public/에 있다면
        // 올바른 경로는 "public/index.html"입니다.
        ClassPathResource html = new ClassPathResource("static/index.html");
        return Mono.just(ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html));
    }
}