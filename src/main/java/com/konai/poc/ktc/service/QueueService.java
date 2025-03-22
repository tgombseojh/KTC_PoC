package com.konai.poc.ktc.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class QueueService {

    private final AtomicInteger connectionCount = new AtomicInteger(0);
    private final Map<String, Integer> sessionOrderMap = new ConcurrentHashMap<>();

    public int assignOrder(String sessionId) {
        int current = connectionCount.incrementAndGet();
        sessionOrderMap.put(sessionId, current);
        return current;
    }

    public void updateOrder(String sessionId, int newOrder) {
        sessionOrderMap.put(sessionId, newOrder);
    }

    public Integer removeSession(String sessionId) {
        connectionCount.decrementAndGet(); // 접속 수 감소
        return sessionOrderMap.remove(sessionId); // 순번 제거
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
}
