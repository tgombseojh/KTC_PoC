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

    private final int MAX_CONNECTIONS = 120000;

    private final QueueService queueService;
    private final Sinks.Many<String> waitingSink = Sinks.many().multicast().directBestEffort();

    int activeSessions;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        activeSessions = queueService.getSessionCount();
        // 최대 접속 초과 시, 거절 메세지 리턴
        if (activeSessions > MAX_CONNECTIONS) {
            //queueService.removeSession(sessionId);
            return session.send(Mono.just(session.textMessage("BUSY")))
                    .then(session.close()); // 메시지 전송 후 연결 종료
        }

        // 세션아이디와 웹소켓세션을 큐에 저장
        String sessionId = session.getId();
        int order = queueService.assignOrder(sessionId, session);

        // 최초 접속 시, 대기 순번을 알려줌
        Mono<Void> sendInitial = session.send(Mono.just(session.textMessage("ORDER:" + order)));


        Flux<Void> inbound = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(msg -> {
                    if (msg.startsWith("PING:")) {
                        session.send(Mono.just(session.textMessage("PONG:" + msg.replace("PING:", "")))).subscribe();
                    }

                    // 순번 계산은 클라이언트가 수행하고 서버에 보고하면 서버의 큐에 해당 정보 갱신
                    if (msg.startsWith("UPDATE:")) {
                        int newOrder = Integer.parseInt(msg.replace("UPDATE:", "").trim());
                        queueService.updateOrder(sessionId, newOrder);
                    }

                    // 누군가가 대기열을 이탈하면 (앱 강제종료 등) 브로드 캐스팅하여 앱이 각자의 순번을 갱신할 수 있도록 함
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
        activeSessions = queueService.getSessionCount();
        waitingSink.tryEmitNext("SESSIONS:" + activeSessions);
        System.out.println("총 대기자 : "+activeSessions);
    }

    @Scheduled(fixedRate = 5000) // 10초마다 실행
    public void cleanDeadSessions() {
        queueService.cleanupDeadSessions();
    }
}
