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

    private final int MAX_CONNECTIONS = 20004;

    private final QueueService queueService;
    private final Sinks.Many<String> waitingSink = Sinks.many().multicast().directBestEffort();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        int order = queueService.assignOrder(sessionId, session);

        int activeSessions = queueService.getSessionCount();
        // 최대 접속 초과 시
        if (activeSessions > MAX_CONNECTIONS) {
            //queueService.removeSession(sessionId);
            return session.send(Mono.just(session.textMessage("BUSY")))
                    .then(session.close()); // 메시지 전송 후 연결 종료
        }

        Mono<Void> sendInitial = session.send(Mono.just(session.textMessage("ORDER:" + order)));

        Flux<Void> inbound = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(msg -> {
                    if (msg.startsWith("PING:")) {
                        session.send(Mono.just(session.textMessage("PONG:" + msg.replace("PING:", "")))).subscribe();
                    }

                    // UPDATE:4 → 순번 갱신
                    if (msg.startsWith("UPDATE:")) {
                        int newOrder = Integer.parseInt(msg.replace("UPDATE:", "").trim());
                        queueService.updateOrder(sessionId, newOrder);
                    }

                    // LEAVE → 나간 사용자 브로드캐스트
                    else if (msg.equals("LEAVE")) {
                        Integer leftOrder = queueService.removeSession(sessionId);
                        if (leftOrder != null) {
                            waitingSink.tryEmitNext("WAITING:" + leftOrder);
                            session.close().subscribe();
                        }
                    }
                })
                .thenMany(Flux.never()); // 수신 스트림을 계속 열어둠


        Flux<WebSocketMessage> outbound = waitingSink.asFlux()
                .map(session::textMessage);

        return sendInitial
                .then(Mono.when(inbound, session.send(outbound)))
                .doFinally(signal -> {

                });
    }


    @Scheduled(fixedRate = 5000)
    public void broadcastSessionCount() {
        int count = queueService.getSessionCount();
        waitingSink.tryEmitNext("SESSIONS:" + count);
        //System.out.println("총 대기자 : "+count);
    }

    @Scheduled(fixedRate = 5000) // 10초마다 실행
    public void cleanDeadSessions() {
        queueService.cleanupDeadSessions();
    }
}
