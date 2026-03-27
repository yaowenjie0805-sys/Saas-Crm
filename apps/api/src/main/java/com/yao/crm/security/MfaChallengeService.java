package com.yao.crm.security;

import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class MfaChallengeService {

    private final Map<String, Challenge> challenges = new ConcurrentHashMap<String, Challenge>();
    private final ScheduledExecutorService sweeper = Executors.newSingleThreadScheduledExecutor();

    public MfaChallengeService() {
        sweeper.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                cleanup();
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    public String issue(String username, String role, String ownerScope, String tenantId) {
        String id = "mfa_" + Long.toString(System.currentTimeMillis(), 36) + String.format("%03d", (int) (Math.random() * 1000));
        Challenge c = new Challenge();
        c.challengeId = id;
        c.username = username;
        c.role = role;
        c.ownerScope = ownerScope;
        c.tenantId = tenantId;
        c.expireAt = LocalDateTime.now().plusMinutes(5);
        challenges.put(id, c);
        return id;
    }

    public Challenge consume(String challengeId) {
        Challenge c = challenges.remove(challengeId);
        if (c == null) return null;
        if (c.expireAt.isBefore(LocalDateTime.now())) return null;
        return c;
    }

    private void cleanup() {
        LocalDateTime now = LocalDateTime.now();
        challenges.values().removeIf(c -> c.expireAt.isBefore(now));
    }

    @PreDestroy
    public void shutdown() {
        sweeper.shutdown();
    }

    public static class Challenge {
        private String challengeId;
        private String username;
        private String role;
        private String ownerScope;
        private String tenantId;
        private LocalDateTime expireAt;

        public String getChallengeId() { return challengeId; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
        public String getOwnerScope() { return ownerScope; }
        public String getTenantId() { return tenantId; }
        public LocalDateTime getExpireAt() { return expireAt; }
    }
}
