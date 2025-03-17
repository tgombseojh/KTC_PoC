package com.konai.poc.ktc.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Controller
public class QueueController {

    // 접속자 수를 관리 (간단한 예제이므로 동시성 처리는 단순함)
    private final AtomicInteger connectionCount = new AtomicInteger(0);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/join")
    public void join(Message<?> message, Principal principal) {
        if (principal == null) {
            return; // 또는 예외 던져도 됨
        }

        int order = connectionCount.incrementAndGet();

        // 세션에 순번 저장
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
        accessor.getSessionAttributes().put("order", order);

        // 사용자에게 개인 순번 전송
        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/order",
                order
        );
    }

    @MessageMapping("/update-order")
    public void updateOrder(Message<?> message, Principal principal) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);

        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs == null) return;

        try {
            String payload = new String((byte[]) message.getPayload());
            int newOrder = Integer.parseInt(payload.replaceAll("[^0-9]", ""));
            attrs.put("order", newOrder);
            //System.out.println("✅ " + principal.getName() + "의 순번이 갱신됨: " + newOrder);
        } catch (Exception e) {
            e.getMessage();
        }
    }


    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        //String sessionId = event.getSessionId();
        //String username = Objects.requireNonNull(event.getUser()).getName();
        //System.out.println("🚫 연결 종료 감지! 세션 ID: " + sessionId);
        //System.out.println("🚫 연결 종료 감지! user: " + username);
        //System.out.println("남은 접속자 수: " + connectionCount.get());

        connectionCount.decrementAndGet();

        // 세션에서 순번 꺼내기
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs != null && attrs.containsKey("order")) {
            int leftOrder = (int) attrs.get("order");

            // 떠난 사용자의 순번 포함해서 메시지 전송
            messagingTemplate.convertAndSend("/topic/waiting", leftOrder);

            //System.out.println("👋 나간 사용자 순번 발행: " + leftOrder);
        } else {
            //System.out.println("❗ 세션에서 순번 정보를 찾을 수 없습니다.");
        }
    }

}
