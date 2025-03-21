package com.konai.poc.ktc.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class QueueService {

    private final AtomicInteger orderCounter = new AtomicInteger(0);
    private final Map<String, Integer> sessionOrderMap = new ConcurrentHashMap<>();

    public int assignOrder(String sessionId) {
        int order = orderCounter.incrementAndGet();
        sessionOrderMap.put(sessionId, order);
        return order;
    }

    public void updateOrder(String sessionId, int newOrder) {
        sessionOrderMap.put(sessionId, newOrder);
    }

    public Integer removeSession(String sessionId) {
        return sessionOrderMap.remove(sessionId);
    }

    public int getSessionCount() {
        return sessionOrderMap.size();
    }

    public Map<String, Integer> getAllOrders() {
        return sessionOrderMap;
    }
}
