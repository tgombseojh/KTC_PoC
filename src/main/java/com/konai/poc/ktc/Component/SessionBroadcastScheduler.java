package com.konai.poc.ktc.Component;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionBroadcastScheduler {

    private final SimpUserRegistry userRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    //@Scheduled(fixedRate = 5000) // 5초마다 실행
    public void broadcastActiveSessionCount() {
        int sessionCount = userRegistry.getUsers().stream()
                .mapToInt(user -> user.getSessions().size())
                .sum();

        // 브로드캐스트 전송
        //messagingTemplate.convertAndSend("/topic/sessions", sessionCount);

        // optional 로그
        //System.out.println("📡 브로드캐스트: 현재 활성 세션 수 = " + sessionCount);
    }
}