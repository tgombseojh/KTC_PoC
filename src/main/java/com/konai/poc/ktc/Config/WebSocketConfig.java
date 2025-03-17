package com.konai.poc.ktc.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 하트비트를 수신/스케줄링할 스레드풀 설정
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("heartbeat-thread-");
        scheduler.setPoolSize(1);
        scheduler.initialize();

        config.enableSimpleBroker("/topic", "/queue")  // topic은 브로드캐스트, queue는 개인 메시지
                .setTaskScheduler(scheduler)
                .setHeartbeatValue(new long[]{0, 5000}); // [서버가 보내는 간격, 서버가 기대하는 클라이언트 하트비트 수신 간격]

        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");  // ✨ 개인 메시지용 prefix
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트가 연결할 엔드포인트를 설정 (SockJS 사용)
        //registry.addEndpoint("/ws").setAllowedOrigins("*").withSockJS();
        registry.addEndpoint("/ws")
                .setHandshakeHandler(new CustomHandshakeHandler()) // ✨ 여기
                .setAllowedOriginPatterns("*")
                .withSockJS();

    }
}
