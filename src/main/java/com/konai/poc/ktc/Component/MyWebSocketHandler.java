package com.konai.poc.ktc.Component;

import com.konai.poc.ktc.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
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

        Mono<Void> sendInitial = session.send(Mono.just(session.textMessage("ORDER:" + order)));

        Flux<Void> inbound = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(msg -> {
                    if (msg.startsWith("UPDATE:")) {
                        int newOrder = Integer.parseInt(msg.replace("UPDATE:", "").trim());
                        queueService.updateOrder(sessionId, newOrder);
                    } else if (msg.equals("LEAVE")) {
                        Integer leftOrder = queueService.removeSession(sessionId);
                        if (leftOrder != null) {
                            waitingSink.tryEmitNext("WAITING:" + leftOrder);
                            session.close().subscribe();
                        }
                    }
                })
                .thenMany(Flux.never());

        Flux<WebSocketMessage> outbound = waitingSink.asFlux()
                .map(session::textMessage);

        return sendInitial
                .then(Mono.when(inbound, session.send(outbound)))
                .doFinally(signal -> {
                    // ✅ 여기에서 이탈 처리
                    Integer leftOrder = queueService.removeSession(sessionId);
                    if (leftOrder != null) {
                        waitingSink.tryEmitNext("WAITING:" + leftOrder);
                        System.out.println("⛔ 연결 종료: " + sessionId + ", 순번: " + leftOrder);
                    }
                });
    }


    @Scheduled(fixedRate = 5000)
    public void broadcastSessionCount() {
        int count = queueService.getSessionCount();
        waitingSink.tryEmitNext("SESSIONS:" + count);
    }
}
