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
import java.util.Optional;
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
    public void updateOrder(Message<?> message) {
    //public void updateOrder(Message<?> message, Principal principal) {
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


    // todo 이탈자가 생기는 즉시 1단위로 이벤트를 전파해서 고객 순번이 차감되는게 옳은가
    //      세션 수도 같이 발행해서, 고객들의 순번이 세션 수 보다 크지 않도록만 하면 될까
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        //String sessionId = event.getSessionId();
        //String username = Objects.requireNonNull(event.getUser()).getName();
        //System.out.println("🚫 연결 종료 감지! 세션 ID: " + sessionId);
        //System.out.println("🚫 연결 종료 감지! user: " + username);
        //System.out.println("남은 접속자 수: " + connectionCount.get());

        connectionCount.decrementAndGet();
        // 너무 많은 연결이 동시에 끊어지는 경우 // 연결이 끊겼으나, 감지되지 않는 경우 // GC동작으로 CPU 사용률이 급증하는 현상


        // 세션에서 순번 꺼내기 (뭉텅이로 이탈하는 경우에는 아웃바운드가 많다)
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Optional.ofNullable(accessor.getSessionAttributes())
                .map(attrs -> attrs.get("order"))
                .filter(val -> val instanceof Integer)
                .map(val -> (Integer) val)
                .filter(order -> order > 0)
                .ifPresent(order -> messagingTemplate.convertAndSend("/topic/waiting", order)); // 브로드캐스팅 채널
    }

}
