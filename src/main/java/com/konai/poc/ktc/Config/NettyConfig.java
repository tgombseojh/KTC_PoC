package com.konai.poc.ktc.Config;

import io.netty.channel.ChannelOption;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.server.HttpServer;
import reactor.netty.tcp.TcpServer;

import java.time.Duration;

@Configuration
public class NettyConfig {

    @Bean
    public ReactiveWebServerFactory reactiveWebServerFactory() {
        NettyReactiveWebServerFactory factory = new NettyReactiveWebServerFactory();

        // ✅ 최신 방식: NettyServerCustomizer 등록
        factory.addServerCustomizers(new NettyServerCustomizer() {
            @Override
            public HttpServer apply(HttpServer httpServer) {
                return httpServer
                        .option(ChannelOption.SO_BACKLOG, 10000)
                        //.option(ChannelOption.SO_KEEPALIVE, true)
                        .idleTimeout(Duration.ofSeconds(10))
                        .httpRequestDecoder(decoderSpec -> decoderSpec.maxHeaderSize(16384));
            }
        });

        return factory;
    }
}
