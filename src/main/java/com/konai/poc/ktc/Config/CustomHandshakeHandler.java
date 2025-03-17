package com.konai.poc.ktc.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Configuration
public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(org.springframework.http.server.ServerHttpRequest request,
                                      org.springframework.web.socket.WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        // 임의의 UUID를 사용자 이름처럼 사용
        return new Principal() {
            private final String name = UUID.randomUUID().toString();

            @Override
            public String getName() {
                return name;
            }
        };
    }
}