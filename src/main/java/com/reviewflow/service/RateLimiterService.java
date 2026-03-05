package com.reviewflow.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 900; // 15 minutes

    private final Map<String, LoginAttemptRecord> attempts = new ConcurrentHashMap<>();

    public void recordFailedLogin(String ipAddress) {
        if (ipAddress == null) return;
        
        attempts.compute(ipAddress, (ip, record) -> {
            Instant now = Instant.now();
            if (record == null || record.windowStart.plusSeconds(WINDOW_SECONDS).isBefore(now)) {
                // Start new window
                return new LoginAttemptRecord(now, 1);
            } else {
                // Increment within same window
                return new LoginAttemptRecord(record.windowStart, record.failedAttempts + 1);
            }
        });
    }

    public boolean isRateLimited(String ipAddress) {
        if (ipAddress == null) return false;
        
        LoginAttemptRecord record = attempts.get(ipAddress);
        if (record == null) return false;
        
        Instant now = Instant.now();
        // Check if window has expired
        if (record.windowStart.plusSeconds(WINDOW_SECONDS).isBefore(now)) {
            attempts.remove(ipAddress);
            return false;
        }
        
        return record.failedAttempts >= MAX_ATTEMPTS;
    }

    public long getRetryAfterSeconds(String ipAddress) {
        if (ipAddress == null) return 0;
        
        LoginAttemptRecord record = attempts.get(ipAddress);
        if (record == null) return 0;
        
        Instant windowEnd = record.windowStart.plusSeconds(WINDOW_SECONDS);
        long secondsUntilReset = windowEnd.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, secondsUntilReset);
    }

    public void clearFailedAttempts(String ipAddress) {
        if (ipAddress != null) {
            attempts.remove(ipAddress);
        }
    }

    private record LoginAttemptRecord(Instant windowStart, int failedAttempts) {}
}
