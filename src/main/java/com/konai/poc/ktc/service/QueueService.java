package com.konai.poc.ktc.service;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class QueueService {

    private final AtomicInteger connectionCount = new AtomicInteger(0);

    // ✅ 세션 ID → 순번
    private final Map<String, Integer> sessionOrderMap = new ConcurrentHashMap<>();

    // ✅ 세션 ID → WebSocketSession
    private final Map<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    public int assignOrder(String sessionId, WebSocketSession session) {
        int current = connectionCount.incrementAndGet();
        sessionOrderMap.put(sessionId, current);
        sessionMap.put(sessionId, session);
        return current;
    }

    public void updateOrder(String sessionId, int newOrder) {
        sessionOrderMap.put(sessionId, newOrder);
    }

    public Integer removeSession(String sessionId) {
        connectionCount.decrementAndGet();
        sessionMap.remove(sessionId);
        return sessionOrderMap.remove(sessionId);
    }

    public int getSessionCount() {
        return connectionCount.get();
    }

    public Integer getOrder(String sessionId) {
        return sessionOrderMap.get(sessionId);
    }

    public Map<String, Integer> getAllOrders() {
        return sessionOrderMap;
    }

    // ✅ 연결이 끊긴 세션 제거 (정리용)
    public void cleanupDeadSessions() {
        sessionMap.forEach((sessionId, session) -> {
            if (!session.isOpen()) {
                removeSession(sessionId);
                //System.out.println("🧹 끊긴 세션 제거됨: " + sessionId);
            }
        });
        System.out.println(" 세션 수: " + sessionMap.size());
    }
}
