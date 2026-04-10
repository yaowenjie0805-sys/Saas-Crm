package com.yao.crm.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.entity.NotificationChannel;
import com.yao.crm.repository.NotificationChannelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

@Service
public class IntegrationWebhookService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationWebhookService.class);
    private static final TypeReference<Map<String, Object>> MAP_REF = new TypeReference<Map<String, Object>>() {};
    private static final String FEISHU_BASE_URL = "https://open.feishu.cn";

    // Circuit breaker configuration
    private static final int FAILURE_THRESHOLD = 5;
    private static final long COOLDOWN_MS = 60_000; // 1 minute
    private static final int HALF_OPEN_MAX_CALLS = 3;
    private static final int HTTP_CONNECT_TIMEOUT_MS = 3_000;
    private static final int HTTP_READ_TIMEOUT_MS = 5_000;
    private static final int WEBHOOK_MAX_RETRIES = 3;
    private static final long WEBHOOK_RETRY_BACKOFF_MS = 300L;

    // Circuit state enum
    private enum CircuitState { CLOSED, OPEN, HALF_OPEN }

    // Per-provider circuit breaker state
    private static class CircuitBreaker {
        private volatile CircuitState state = CircuitState.CLOSED;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger halfOpenSuccessCount = new AtomicInteger(0);
        private volatile long lastFailureTime = 0;

        void recordSuccess() {
            if (state == CircuitState.HALF_OPEN) {
                if (halfOpenSuccessCount.incrementAndGet() >= HALF_OPEN_MAX_CALLS) {
                    reset();
                }
            } else {
                failureCount.set(0);
            }
        }

        void recordFailure() {
            lastFailureTime = System.currentTimeMillis();
            if (state == CircuitState.HALF_OPEN) {
                transitionToOpen();
            } else if (failureCount.incrementAndGet() >= FAILURE_THRESHOLD) {
                transitionToOpen();
            }
        }

        boolean allowRequest() {
            if (state == CircuitState.CLOSED) {
                return true;
            }
            if (state == CircuitState.OPEN) {
                if (System.currentTimeMillis() - lastFailureTime > COOLDOWN_MS) {
                    transitionToHalfOpen();
                    return true;
                }
                return false;
            }
            // HALF_OPEN state - allow limited requests
            return halfOpenSuccessCount.get() < HALF_OPEN_MAX_CALLS;
        }

        private void transitionToOpen() {
            state = CircuitState.OPEN;
            failureCount.set(0);
            halfOpenSuccessCount.set(0);
            log.warn("Circuit breaker opened for provider");
        }

        private void transitionToHalfOpen() {
            state = CircuitState.HALF_OPEN;
            halfOpenSuccessCount.set(0);
            log.info("Circuit breaker transitioned to half-open");
        }

        private void reset() {
            state = CircuitState.CLOSED;
            failureCount.set(0);
            halfOpenSuccessCount.set(0);
            log.info("Circuit breaker reset to closed");
        }

        CircuitState getState() {
            return state;
        }
    }

    private final NotificationChannelRepository notificationChannelRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    private final String wecomWebhookUrl;
    private final String wecomSecret;
    private final String dingTalkWebhookUrl;
    private final String dingTalkSecret;

    private final String feishuWebhookUrl;
    private final String feishuSecret;
    private final String feishuAppId;
    private final String feishuAppSecret;
    private final String feishuReceiveId;
    private final String feishuReceiveIdType;
    private final String feishuBaseUrl;
    private final Set<String> allowedWebhookHostSuffixes;

    // Circuit breakers per provider
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    public static final class DispatchResult {
        private final boolean success;
        private final boolean retryable;
        private final int statusCode;
        private final int attempts;

        private DispatchResult(boolean success, boolean retryable, int statusCode, int attempts) {
            this.success = success;
            this.retryable = retryable;
            this.statusCode = statusCode;
            this.attempts = attempts;
        }

        public static DispatchResult success(int attempts, int statusCode, boolean retryable) {
            return new DispatchResult(true, retryable, statusCode, attempts);
        }

        public static DispatchResult failure(int attempts, int statusCode, boolean retryable) {
            return new DispatchResult(false, retryable, statusCode, attempts);
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isRetryable() {
            return retryable;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public int getAttempts() {
            return attempts;
        }
    }

    public IntegrationWebhookService(NotificationChannelRepository notificationChannelRepository,
                                     ObjectMapper objectMapper,
                                     RestTemplateBuilder restTemplateBuilder,
                                     @Value("${integration.wecom.webhook-url:}") String wecomWebhookUrl,
                                     @Value("${integration.wecom.secret:}") String wecomSecret,
                                     @Value("${integration.dingtalk.webhook-url:}") String dingTalkWebhookUrl,
                                     @Value("${integration.dingtalk.secret:}") String dingTalkSecret,
                                     @Value("${integration.feishu.webhook-url:}") String feishuWebhookUrl,
                                     @Value("${integration.feishu.secret:}") String feishuSecret,
                                     @Value("${integration.feishu.app-id:}") String feishuAppId,
                                     @Value("${integration.feishu.app-secret:}") String feishuAppSecret,
                                     @Value("${integration.feishu.receive-id:}") String feishuReceiveId,
                                     @Value("${integration.feishu.receive-id-type:chat_id}") String feishuReceiveIdType,
                                     @Value("${integration.feishu.base-url:https://open.feishu.cn}") String feishuBaseUrl,
                                     @Value("${integration.webhook.allowed-host-suffixes:}") String allowedWebhookHostSuffixes) {
        this.notificationChannelRepository = notificationChannelRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(HTTP_CONNECT_TIMEOUT_MS))
                .setReadTimeout(Duration.ofMillis(HTTP_READ_TIMEOUT_MS))
                .build();
        this.allowedWebhookHostSuffixes = parseAllowedHostSuffixes(allowedWebhookHostSuffixes);

        this.wecomWebhookUrl = sanitizeWebhookUrl(wecomWebhookUrl);
        this.wecomSecret = safeTrim(wecomSecret);
        this.dingTalkWebhookUrl = sanitizeWebhookUrl(dingTalkWebhookUrl);
        this.dingTalkSecret = safeTrim(dingTalkSecret);

        this.feishuWebhookUrl = sanitizeWebhookUrl(feishuWebhookUrl);
        this.feishuSecret = safeTrim(feishuSecret);
        this.feishuAppId = safeTrim(feishuAppId);
        this.feishuAppSecret = safeTrim(feishuAppSecret);
        this.feishuReceiveId = safeTrim(feishuReceiveId);
        this.feishuReceiveIdType = normalizeReceiveIdType(feishuReceiveIdType);
        this.feishuBaseUrl = sanitizeBaseUrl(feishuBaseUrl);

        // Initialize circuit breakers for each provider
        circuitBreakers.put("WECOM", new CircuitBreaker());
        circuitBreakers.put("DINGTALK", new CircuitBreaker());
        circuitBreakers.put("FEISHU", new CircuitBreaker());
    }

    public boolean sendMessage(String provider, String tenantId, String title, String content, String userId) {
        return sendMessageDetailed(provider, tenantId, title, content, userId).isSuccess();
    }

    public DispatchResult sendMessageDetailed(String provider, String tenantId, String title, String content, String userId) {
        String normalized = normalizeProvider(provider);
        CircuitBreaker circuitBreaker = circuitBreakers.get(normalized);

        // Check circuit breaker before attempting any call
        if (circuitBreaker != null && !circuitBreaker.allowRequest()) {
            log.warn("Circuit breaker is open for provider={}, skipping webhook call", normalized);
            return DispatchResult.failure(0, -1, false);
        }

        ProviderConfig config = resolveProviderConfig(normalized, tenantId);
        String text = safeText(title, "CRM Notification") + "\n" + safeText(content, "");

        boolean success = false;
        DispatchResult dispatchResult = DispatchResult.failure(0, -1, false);

        if ("FEISHU".equals(normalized) && canUseFeishuApp(config)) {
            boolean appSent = sendFeishuByApp(config, text, userId);
            if (appSent) {
                success = true;
                dispatchResult = DispatchResult.success(1, 200, false);
            } else {
                log.warn("Feishu app mode failed, fallback to webhook if available. tenantId={}", tenantId);
            }
        }

        String webhookUrl = sanitizeWebhookUrl(config.webhookUrl);
        if (!success && !isBlank(webhookUrl)) {
            Map<String, Object> body = buildProviderMessageBody(normalized, title, content, config.secret);
            String finalUrl = buildSignedUrl(normalized, webhookUrl, config.secret);
            dispatchResult = postJson(finalUrl, body);
            success = dispatchResult.isSuccess();
        } else if (isBlank(webhookUrl)) {
            log.warn("Webhook skipped because url is missing. provider={}, tenantId={}", normalized, tenantId);
        }

        // Record result in circuit breaker
        if (circuitBreaker != null) {
            if (success) {
                circuitBreaker.recordSuccess();
            } else {
                circuitBreaker.recordFailure();
            }
        }

        return dispatchResult;
    }

    public boolean sendEvent(String provider, String tenantId, String eventType, String payloadJson, String jobId) {
        return sendEventDetailed(provider, tenantId, eventType, payloadJson, jobId).isSuccess();
    }

    public DispatchResult sendEventDetailed(String provider, String tenantId, String eventType, String payloadJson, String jobId) {
        String title = "CRM Notification Event: " + safeText(eventType, "unknown");
        String content = "jobId=" + safeText(jobId, "-") + "\n" + safeText(payloadJson, "{}");
        return sendMessageDetailed(provider, tenantId, title, content, null);
    }

    private boolean sendFeishuByApp(ProviderConfig config, String text, String userId) {
        String token = fetchFeishuTenantAccessToken(config);
        if (isBlank(token)) {
            return false;
        }

        String receiveId = firstNonBlank(config.receiveId, userId);
        if (isBlank(receiveId)) {
            log.warn("Feishu app mode skipped because receiveId is missing.");
            return false;
        }

        try {
            String receiveIdType = normalizeReceiveIdType(config.receiveIdType);
            String base = isBlank(config.baseUrl) ? FEISHU_BASE_URL : config.baseUrl;
            String url = base + "/open-apis/im/v1/messages?receive_id_type=" + URLEncoder.encode(receiveIdType, "UTF-8");

            Map<String, Object> contentObj = new LinkedHashMap<String, Object>();
            contentObj.put("text", text);

            Map<String, Object> body = new LinkedHashMap<String, Object>();
            body.put("receive_id", receiveId);
            body.put("msg_type", "text");
            body.put("content", objectMapper.writeValueAsString(contentObj));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<Map<String, Object>>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            int status = response.getStatusCodeValue();
            if (status < 200 || status >= 300) {
                log.warn("Feishu app send returned non-2xx. status={}, url={}", status, maskedUrl(url));
                return false;
            }

            JsonNode root = objectMapper.readTree(response.getBody() == null ? "{}" : response.getBody());
            int code = root.path("code").asInt(-1);
            if (code == 0) {
                return true;
            }
            log.warn("Feishu app send rejected. code={}, msg={}", code, root.path("msg").asText(""));
            return false;
        } catch (Exception ex) {
            log.error("Feishu app send failed: {}", ex.getMessage());
            return false;
        }
    }

    private String fetchFeishuTenantAccessToken(ProviderConfig config) {
        if (isBlank(config.appId) || isBlank(config.appSecret)) {
            return "";
        }

        try {
            String base = isBlank(config.baseUrl) ? FEISHU_BASE_URL : config.baseUrl;
            String url = base + "/open-apis/auth/v3/tenant_access_token/internal";

            Map<String, Object> body = new LinkedHashMap<String, Object>();
            body.put("app_id", config.appId);
            body.put("app_secret", config.appSecret);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<Map<String, Object>>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            int status = response.getStatusCodeValue();
            if (status < 200 || status >= 300) {
                log.warn("Feishu token endpoint returned non-2xx. status={}, url={}", status, maskedUrl(url));
                return "";
            }

            JsonNode root = objectMapper.readTree(response.getBody() == null ? "{}" : response.getBody());
            int code = root.path("code").asInt(-1);
            if (code != 0) {
                log.warn("Feishu token request rejected. code={}, msg={}", code, root.path("msg").asText(""));
                return "";
            }
            return safeTrim(root.path("tenant_access_token").asText(""));
        } catch (Exception ex) {
            log.error("Fetch Feishu tenant access token failed: {}", ex.getMessage());
            return "";
        }
    }

    private Map<String, Object> buildProviderMessageBody(String provider, String title, String content, String secret) {
        String line = safeText(title, "CRM Notification") + "\n" + safeText(content, "");
        if ("DINGTALK".equals(provider)) {
            Map<String, Object> body = new LinkedHashMap<String, Object>();
            body.put("msgtype", "markdown");
            Map<String, Object> markdown = new LinkedHashMap<String, Object>();
            markdown.put("title", safeText(title, "CRM Notification"));
            markdown.put("text", line);
            body.put("markdown", markdown);
            return body;
        }
        if ("FEISHU".equals(provider)) {
            Map<String, Object> body = new LinkedHashMap<String, Object>();
            body.put("msg_type", "text");
            Map<String, Object> contentObj = new LinkedHashMap<String, Object>();
            contentObj.put("text", line);
            body.put("content", contentObj);
            if (!isBlank(secret)) {
                long ts = Instant.now().getEpochSecond();
                body.put("timestamp", String.valueOf(ts));
                body.put("sign", buildFeishuSign(ts, secret));
            }
            return body;
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("msgtype", "markdown");
        Map<String, Object> markdown = new LinkedHashMap<String, Object>();
        markdown.put("content", line);
        body.put("markdown", markdown);
        return body;
    }

    private String buildSignedUrl(String provider, String webhookUrl, String secret) {
        if ("DINGTALK".equals(provider) && !isBlank(secret)) {
            long ts = Instant.now().toEpochMilli();
            String sign = buildDingTalkSign(ts, secret);
            String join = webhookUrl.contains("?") ? "&" : "?";
            return webhookUrl + join + "timestamp=" + ts + "&sign=" + sign;
        }
        return webhookUrl;
    }

    private String buildDingTalkSign(long timestamp, String secret) {
        String stringToSign = timestamp + "\n" + secret;
        byte[] signData = hmacSha256(secret, stringToSign);
        String base64 = Base64.getEncoder().encodeToString(signData);
        try {
            return URLEncoder.encode(base64, "UTF-8");
        } catch (Exception ex) {
            return base64;
        }
    }

    private String buildFeishuSign(long timestamp, String secret) {
        String stringToSign = timestamp + "\n" + secret;
        byte[] signData = hmacSha256(secret, stringToSign);
        return Base64.getEncoder().encodeToString(signData);
    }

    private byte[] hmacSha256(String secret, String text) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build webhook signature", ex);
        }
    }

    private DispatchResult postJson(String url, Map<String, Object> body) {
        boolean retryable = false;
        for (int attempt = 1; attempt <= WEBHOOK_MAX_RETRIES; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<Map<String, Object>>(body, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
                int status = response.getStatusCodeValue();
                if (status >= 200 && status < 300) {
                    return DispatchResult.success(attempt, status, retryable);
                }
                boolean currentRetryable = isRetryableStatus(status);
                retryable = retryable || currentRetryable;
                boolean canRetry = currentRetryable && attempt < WEBHOOK_MAX_RETRIES;
                if (canRetry) {
                    backoff(attempt);
                    continue;
                }
                log.warn("Webhook dispatch returned non-2xx. status={}, url={}, attempt={}", status, maskedUrl(url), attempt);
                return DispatchResult.failure(attempt, status, retryable);
            } catch (Exception ex) {
                retryable = true;
                boolean canRetry = attempt < WEBHOOK_MAX_RETRIES;
                if (canRetry) {
                    log.warn("Webhook dispatch attempt failed, will retry. url={}, attempt={}, error={}", maskedUrl(url), attempt, ex.getMessage());
                    backoff(attempt);
                    continue;
                }
                log.error("Webhook dispatch failed. url={}, attempt={}, error={}", maskedUrl(url), attempt, ex.getMessage());
                return DispatchResult.failure(attempt, -1, true);
            }
        }
        return DispatchResult.failure(WEBHOOK_MAX_RETRIES, -1, retryable);
    }

    private boolean isRetryableStatus(int status) {
        return status == 429 || status >= 500;
    }

    private void backoff(int attempt) {
        long delayMs = WEBHOOK_RETRY_BACKOFF_MS * attempt;
        LockSupport.parkNanos(java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(delayMs));
        if (Thread.currentThread().isInterrupted()) {
            log.warn("Webhook retry backoff interrupted");
            Thread.currentThread().interrupt();
        }
    }

    private boolean canUseFeishuApp(ProviderConfig config) {
        return !isBlank(config.appId) && !isBlank(config.appSecret);
    }

    private ProviderConfig resolveProviderConfig(String provider, String tenantId) {
        ProviderConfig base = defaultsByProvider(provider);
        ProviderConfig byDb = resolveFromNotificationChannel(provider, tenantId);
        return base.merge(byDb);
    }

    private ProviderConfig defaultsByProvider(String provider) {
        if ("DINGTALK".equals(provider)) {
            return new ProviderConfig(dingTalkWebhookUrl, dingTalkSecret, "", "", "", "", "");
        }
        if ("FEISHU".equals(provider)) {
            return new ProviderConfig(feishuWebhookUrl, feishuSecret, feishuAppId, feishuAppSecret,
                    feishuReceiveId, feishuReceiveIdType, feishuBaseUrl);
        }
        return new ProviderConfig(wecomWebhookUrl, wecomSecret, "", "", "", "", "");
    }

    private ProviderConfig resolveFromNotificationChannel(String provider, String tenantId) {
        String channelType = toChannelType(provider);
        if (isBlank(channelType) || isBlank(tenantId)) {
            return ProviderConfig.empty();
        }
        List<NotificationChannel> channels = notificationChannelRepository.findTopByTenantAndChannelType(tenantId, channelType);
        for (NotificationChannel channel : channels) {
            if (channel == null || channel.getEnabled() == null || !channel.getEnabled()) {
                continue;
            }
            Map<String, Object> cfg = parseConfig(channel.getConfigJson());
            String url = readString(cfg, "webhookUrl", "url", "webhook", "endpoint", "webhook_url");
            String secret = readString(cfg, "secret", "signSecret", "signatureSecret", "sign_secret");
            String appId = readString(cfg, "appId", "app_id");
            String appSecret = readString(cfg, "appSecret", "app_secret");
            String receiveId = readString(cfg, "receiveId", "receive_id", "chatId", "chat_id");
            String receiveIdType = readString(cfg, "receiveIdType", "receive_id_type");
            String baseUrl = readString(cfg, "baseUrl", "base_url");
            return new ProviderConfig(
                    sanitizeWebhookUrl(url),
                    secret,
                    appId,
                    appSecret,
                    receiveId,
                    receiveIdType,
                    sanitizeBaseUrl(baseUrl)
            );
        }
        return ProviderConfig.empty();
    }

    String sanitizeWebhookUrl(String rawUrl) {
        return sanitizeOutboundUrl(rawUrl, "webhook-url");
    }

    String sanitizeBaseUrl(String rawUrl) {
        return sanitizeOutboundUrl(rawUrl, "base-url");
    }

    private String sanitizeOutboundUrl(String rawUrl, String fieldName) {
        String value = safeTrim(rawUrl);
        if (isBlank(value)) {
            return "";
        }
        try {
            URI uri = URI.create(value);
            String scheme = safeTrim(uri.getScheme()).toLowerCase(Locale.ROOT);
            String host = safeTrim(uri.getHost()).toLowerCase(Locale.ROOT);
            if (!"https".equals(scheme)) {
                log.warn("Blocked outbound {} because non-https scheme was provided: {}", fieldName, maskedUrl(value));
                return "";
            }
            if (isBlank(host) || isLocalOrPrivateHost(host)) {
                log.warn("Blocked outbound {} because host is local/private: {}", fieldName, maskedUrl(value));
                return "";
            }
            if (!isAllowedHostSuffix(host)) {
                log.warn("Blocked outbound {} because host is not in allowlist: {}", fieldName, maskedUrl(value));
                return "";
            }
            return value;
        } catch (Exception ex) {
            log.warn("Blocked outbound {} because URL is invalid: {}", fieldName, maskedUrl(value));
            return "";
        }
    }

    private Set<String> parseAllowedHostSuffixes(String raw) {
        String value = safeTrim(raw);
        if (isBlank(value)) {
            return Collections.emptySet();
        }
        Set<String> out = new HashSet<String>();
        String[] tokens = value.split("[,;\\s]+");
        for (String token : tokens) {
            String suffix = safeTrim(token).toLowerCase(Locale.ROOT);
            if (!suffix.isEmpty()) {
                out.add(suffix);
            }
        }
        return out;
    }

    private boolean isAllowedHostSuffix(String host) {
        if (allowedWebhookHostSuffixes.isEmpty()) {
            return true;
        }
        for (String suffix : allowedWebhookHostSuffixes) {
            if (host.equals(suffix) || host.endsWith("." + suffix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLocalOrPrivateHost(String host) {
        if ("localhost".equals(host)) {
            return true;
        }
        if (isIpv6Loopback(host)) {
            return true;
        }
        if (!host.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
            return false;
        }
        String[] parts = host.split("\\.");
        if (parts.length != 4) {
            return true;
        }
        int a = parseInt(parts[0]);
        int b = parseInt(parts[1]);
        if (a < 0 || b < 0) {
            return true;
        }
        if (a == 10 || a == 127 || a == 0) {
            return true;
        }
        if (a == 169 && b == 254) {
            return true;
        }
        if (a == 172 && b >= 16 && b <= 31) {
            return true;
        }
        if (a == 192 && b == 168) {
            return true;
        }
        return false;
    }

    private boolean isIpv6Loopback(String host) {
        String normalized = safeTrim(host);
        if (normalized.startsWith("[") && normalized.endsWith("]") && normalized.length() > 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return "::1".equals(normalized) || "0:0:0:0:0:0:0:1".equals(normalized);
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return -1;
        }
    }

    private Map<String, Object> parseConfig(String json) {
        if (isBlank(json)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, MAP_REF);
        } catch (Exception ex) {
            log.warn("Failed to parse notification channel config json: {}", ex.getMessage());
            return Collections.emptyMap();
        }
    }

    private String readString(Map<String, Object> map, String... keys) {
        if (map == null || keys == null) {
            return "";
        }
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof String && !isBlank((String) value)) {
                return ((String) value).trim();
            }
        }
        return "";
    }

    private String normalizeProvider(String provider) {
        String p = safeTrim(provider).toUpperCase(Locale.ROOT);
        if ("WECHAT_WORK".equals(p) || "WECOM".equals(p)) return "WECOM";
        if ("DING".equals(p) || "DINGTALK".equals(p)) return "DINGTALK";
        if ("LARK".equals(p) || "FEISHU".equals(p)) return "FEISHU";
        return p.isEmpty() ? "WECOM" : p;
    }

    private String toChannelType(String provider) {
        if ("WECOM".equals(provider)) return "WECHAT_WORK";
        if ("DINGTALK".equals(provider)) return "DINGTALK";
        if ("FEISHU".equals(provider)) return "FEISHU";
        return "";
    }

    private String normalizeReceiveIdType(String receiveIdType) {
        String value = safeTrim(receiveIdType).toLowerCase(Locale.ROOT);
        if ("user_id".equals(value) || "union_id".equals(value) || "open_id".equals(value) || "chat_id".equals(value)) {
            return value;
        }
        return "chat_id";
    }

    private String safeText(String value, String fallback) {
        String text = value == null ? "" : value.trim();
        return text.isEmpty() ? fallback : text;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String currentRequestId() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
            if (request != null) {
                Object trace = request.getAttribute(com.yao.crm.security.TraceIdInterceptor.TRACE_ID_ATTR);
                if (trace != null && !isBlank(String.valueOf(trace))) {
                    return String.valueOf(trace);
                }
            }
        }
        return "system";
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String v : values) {
            if (!isBlank(v)) {
                return safeTrim(v);
            }
        }
        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String maskedUrl(String url) {
        if (isBlank(url)) return "<empty>";
        int idx = url.indexOf('?');
        return idx >= 0 ? url.substring(0, idx) + "?***" : url;
    }

    private static class ProviderConfig {
        private final String webhookUrl;
        private final String secret;
        private final String appId;
        private final String appSecret;
        private final String receiveId;
        private final String receiveIdType;
        private final String baseUrl;

        private ProviderConfig(String webhookUrl, String secret,
                               String appId, String appSecret,
                               String receiveId, String receiveIdType,
                               String baseUrl) {
            this.webhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
            this.secret = secret == null ? "" : secret.trim();
            this.appId = appId == null ? "" : appId.trim();
            this.appSecret = appSecret == null ? "" : appSecret.trim();
            this.receiveId = receiveId == null ? "" : receiveId.trim();
            this.receiveIdType = receiveIdType == null ? "" : receiveIdType.trim();
            this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        }

        private ProviderConfig merge(ProviderConfig override) {
            if (override == null) {
                return this;
            }
            return new ProviderConfig(
                    pick(this.webhookUrl, override.webhookUrl),
                    pick(this.secret, override.secret),
                    pick(this.appId, override.appId),
                    pick(this.appSecret, override.appSecret),
                    pick(this.receiveId, override.receiveId),
                    pick(this.receiveIdType, override.receiveIdType),
                    pick(this.baseUrl, override.baseUrl)
            );
        }

        private static ProviderConfig empty() {
            return new ProviderConfig("", "", "", "", "", "", "");
        }

        private static String pick(String base, String override) {
            if (override != null && !override.trim().isEmpty()) {
                return override.trim();
            }
            return base == null ? "" : base.trim();
        }
    }
}
