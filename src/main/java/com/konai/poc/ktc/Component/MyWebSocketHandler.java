package com.konai.poc.ktc.Component;

import com.konai.poc.ktc.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Component
@RequiredArgsConstructor
public class MyWebSocketHandler implements WebSocketHandler {

    private final QueueService queueService;
    private final Sinks.Many<String> waitingSink = Sinks.many().multicast().onBackpressureBuffer();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        int order = queueService.assignOrder(sessionId);

        System.out.println("✅ 연결됨: " + sessionId + ", 순번: " + order);

        // 👉 최초 순번 전송
        Mono<Void> sendInitial = session.send(Mono.just(session.textMessage("ORDER:" + order)));

        // 👉 클라이언트로부터 메시지 수신 처리
        Flux<Void> inbound = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(msg -> {
                    if (msg.startsWith("UPDATE:")) {
                        int newOrder = Integer.parseInt(msg.replace("UPDATE:", "").trim());
                        queueService.updateOrder(sessionId, newOrder);
                    } else if (msg.equals("LEAVE")) {
                        queueService.removeSession(sessionId);
                        waitingSink.tryEmitNext("LEAVE:" + sessionId);
                        session.close().subscribe();
                    }
                })
                .thenMany(Flux.never()); // 수신 스트림을 끊지 않음

        // 👉 브로드캐스트 메시지 수신 처리 (다른 유저의 이탈 등)
        Flux<WebSocketMessage> outbound = waitingSink.asFlux()
                .map(msg -> session.textMessage(msg));

        // 👉 실제 송신
        Mono<Void> sendStream = session.send(outbound);

        return sendInitial
                .then(Mono.when(inbound, sendStream)) // 수신 + 송신 병렬 실행
                .doFinally(signal -> {
                    Integer removed = queueService.removeSession(sessionId);
                    if (removed != null) {
                        System.out.println("⛔ 연결 종료: " + sessionId);
                        waitingSink.tryEmitNext("WAITING:" + removed);
                    }
                });
    }
}
