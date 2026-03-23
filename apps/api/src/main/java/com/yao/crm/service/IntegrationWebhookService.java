package com.yao.crm.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.entity.NotificationChannel;
import com.yao.crm.repository.NotificationChannelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class IntegrationWebhookService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationWebhookService.class);
    private static final TypeReference<Map<String, Object>> MAP_REF = new TypeReference<Map<String, Object>>() {};
    private static final String FEISHU_BASE_URL = "https://open.feishu.cn";

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

    public IntegrationWebhookService(NotificationChannelRepository notificationChannelRepository,
                                     ObjectMapper objectMapper,
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
                                     @Value("${integration.feishu.base-url:https://open.feishu.cn}") String feishuBaseUrl) {
        this.notificationChannelRepository = notificationChannelRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();

        this.wecomWebhookUrl = safeTrim(wecomWebhookUrl);
        this.wecomSecret = safeTrim(wecomSecret);
        this.dingTalkWebhookUrl = safeTrim(dingTalkWebhookUrl);
        this.dingTalkSecret = safeTrim(dingTalkSecret);

        this.feishuWebhookUrl = safeTrim(feishuWebhookUrl);
        this.feishuSecret = safeTrim(feishuSecret);
        this.feishuAppId = safeTrim(feishuAppId);
        this.feishuAppSecret = safeTrim(feishuAppSecret);
        this.feishuReceiveId = safeTrim(feishuReceiveId);
        this.feishuReceiveIdType = normalizeReceiveIdType(feishuReceiveIdType);
        this.feishuBaseUrl = safeTrim(feishuBaseUrl);
    }

    public boolean sendMessage(String provider, String tenantId, String title, String content, String userId) {
        String normalized = normalizeProvider(provider);
        ProviderConfig config = resolveProviderConfig(normalized, tenantId);
        String text = safeText(title, "CRM Notification") + "\n" + safeText(content, "");

        if ("FEISHU".equals(normalized) && canUseFeishuApp(config)) {
            boolean appSent = sendFeishuByApp(config, text, userId);
            if (appSent) {
                return true;
            }
            log.warn("Feishu app mode failed, fallback to webhook if available. tenantId={}", tenantId);
        }

        if (isBlank(config.webhookUrl)) {
            log.warn("Webhook skipped because url is missing. provider={}, tenantId={}", normalized, tenantId);
            return false;
        }

        Map<String, Object> body = buildProviderMessageBody(normalized, title, content, config.secret);
        String finalUrl = buildSignedUrl(normalized, config.webhookUrl, config.secret);
        return postJson(finalUrl, body);
    }

    public boolean sendEvent(String provider, String tenantId, String eventType, String payloadJson, String jobId) {
        String title = "CRM Notification Event: " + safeText(eventType, "unknown");
        String content = "jobId=" + safeText(jobId, "-") + "\n" + safeText(payloadJson, "{}");
        return sendMessage(provider, tenantId, title, content, null);
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

    private boolean postJson(String url, Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<Map<String, Object>>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            int status = response.getStatusCodeValue();
            if (status >= 200 && status < 300) {
                return true;
            }
            log.warn("Webhook dispatch returned non-2xx. status={}, url={}", status, maskedUrl(url));
            return false;
        } catch (Exception ex) {
            log.error("Webhook dispatch failed. url={}, error={}", maskedUrl(url), ex.getMessage());
            return false;
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
            return new ProviderConfig(url, secret, appId, appSecret, receiveId, receiveIdType, baseUrl);
        }
        return ProviderConfig.empty();
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
