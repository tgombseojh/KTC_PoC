package com.konai.poc.ktc.Config;

import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class NettyConfig {

    @Bean
    public NettyReactiveWebServerFactory nettyReactiveWebServerFactory() {
        NettyReactiveWebServerFactory factory = new NettyReactiveWebServerFactory();

        factory.addServerCustomizers(httpServer ->
                httpServer
                        .idleTimeout(Duration.ofSeconds(10))  // ✅ 유휴 상태 10초 이상 → 커넥션 자동 종료
        );

        return factory;
    }
}
