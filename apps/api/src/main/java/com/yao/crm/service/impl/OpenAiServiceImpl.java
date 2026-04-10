package com.yao.crm.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.config.AiConfig;
import com.yao.crm.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * OpenAI GPT implementation.
 */
@Service
@Slf4j
public class OpenAiServiceImpl implements AiService {

    private static final String OPENAI_FAILURE_MESSAGE = "AI service is temporarily unavailable. Please try again later.";
    private static final String OPENAI_NOT_CONFIGURED_MESSAGE = "AI service is not configured. Please contact administrator.";
    private static final int MAX_LOG_RESPONSE_LENGTH = 400;

    private final AiConfig aiConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final int maxRetries;
    private final long retryBackoffMs;
    private final RetrySleeper retrySleeper;

    @Autowired
    public OpenAiServiceImpl(
            AiConfig aiConfig,
            ObjectMapper objectMapper,
            RestTemplateBuilder restTemplateBuilder,
            @Value("${ai.openai.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${ai.openai.read-timeout-ms:15000}") int readTimeoutMs,
            @Value("${ai.openai.max-retries:2}") int maxRetries,
            @Value("${ai.openai.retry-backoff-ms:200}") long retryBackoffMs
    ) {
        this(
                aiConfig,
                restTemplateBuilder
                        .setConnectTimeout(Duration.ofMillis(Math.max(1, connectTimeoutMs)))
                        .setReadTimeout(Duration.ofMillis(Math.max(1, readTimeoutMs)))
                        .build(),
                objectMapper,
                maxRetries,
                retryBackoffMs,
                Thread::sleep
        );
    }

    OpenAiServiceImpl(
            AiConfig aiConfig,
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            int maxRetries,
            long retryBackoffMs,
            RetrySleeper retrySleeper
    ) {
        this.aiConfig = aiConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.maxRetries = Math.max(0, maxRetries);
        this.retryBackoffMs = Math.max(0L, retryBackoffMs);
        this.retrySleeper = retrySleeper;
    }

    /** RestTemplate now created via RestTemplateBuilder in constructor */

    @Override
    public String generateText(String prompt) {
        return generateText(prompt, Map.of("temperature", 0.7));
    }

    @Override
    public String generateText(String prompt, Map<String, Object> options) {
        Map<String, Object> normalizedOptions = options == null ? Map.of() : options;
        String apiKey = resolveApiKey(normalizedOptions);
        if (apiKey.isEmpty()) {
            log.warn("OpenAI API key not configured");
            return OPENAI_NOT_CONFIGURED_MESSAGE;
        }

        String url = resolveEndpointUrl(normalizedOptions);
        HttpEntity<Map<String, Object>> entity = buildRequestEntity(prompt, normalizedOptions, apiKey);
        int totalAttempts = maxRetries + 1;

        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        String.class
                );
                return parseOpenAiResponse(response.getBody());
            } catch (HttpStatusCodeException e) {
                HttpStatus status = e.getStatusCode();
                boolean retryable = isRetryable(status);
                String bodySnippet = abbreviateForLog(e.getResponseBodyAsString());

                if (retryable && attempt < totalAttempts) {
                    long delayMs = calculateBackoffMs(attempt);
                    log.warn("OpenAI call retryable failure status={} attempt={}/{} delayMs={} responseSnippet={}",
                            status.value(), attempt, totalAttempts, delayMs, bodySnippet);
                    if (!sleepBeforeRetry(delayMs)) {
                        return OPENAI_FAILURE_MESSAGE;
                    }
                    continue;
                }

                log.error("OpenAI call failed status={} attempt={}/{} responseSnippet={}",
                        status.value(), attempt, totalAttempts, bodySnippet, e);
                return OPENAI_FAILURE_MESSAGE;
            } catch (Exception e) {
                log.error("OpenAI call failed attempt={}/{} errorType={} message={}",
                        attempt, totalAttempts, e.getClass().getSimpleName(), e.getMessage(), e);
                return OPENAI_FAILURE_MESSAGE;
            }
        }

        return OPENAI_FAILURE_MESSAGE;
    }

    private HttpEntity<Map<String, Object>> buildRequestEntity(String prompt, Map<String, Object> normalizedOptions, String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        String overrideModel = normalizedOptions.get("model") == null ? "" : String.valueOf(normalizedOptions.get("model")).trim();
        requestBody.put("model", overrideModel.isEmpty() ? aiConfig.getOpenai().getModel() : overrideModel);
        requestBody.put("messages", new Object[]{Map.of("role", "user", "content", prompt)});
        requestBody.put("temperature", normalizedOptions.getOrDefault("temperature", 0.7));
        return new HttpEntity<>(requestBody, headers);
    }

    private String resolveEndpointUrl(Map<String, Object> options) {
        String configuredBase = aiConfig.getOpenai().getBaseUrl();
        String overrideBase = options.get("base_url") == null ? "" : String.valueOf(options.get("base_url")).trim();
        String base = overrideBase.isEmpty() ? configuredBase : overrideBase;
        String normalizedBase = trimTrailingSlash(base == null ? "" : base.trim());
        if (normalizedBase.endsWith("/chat/completions")) {
            return normalizedBase;
        }
        if (normalizedBase.endsWith("/v1")) {
            return normalizedBase + "/chat/completions";
        }
        return normalizedBase + "/v1/chat/completions";
    }

    private String trimTrailingSlash(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/") && normalized.length() > 0) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String resolveApiKey(Map<String, Object> options) {
        String overrideApiKey = options.get("api_key") == null ? "" : String.valueOf(options.get("api_key")).trim();
        if (!overrideApiKey.isEmpty()) {
            return overrideApiKey;
        }
        String configured = aiConfig.getOpenai().getApiKey();
        return configured == null ? "" : configured.trim();
    }

    private boolean isRetryable(HttpStatus status) {
        return status == HttpStatus.TOO_MANY_REQUESTS || status.is5xxServerError();
    }

    private long calculateBackoffMs(int attempt) {
        return retryBackoffMs * attempt;
    }

    private boolean sleepBeforeRetry(long delayMs) {
        try {
            retrySleeper.sleep(delayMs);
            return true;
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            log.warn("OpenAI retry sleep interrupted");
            return false;
        }
    }

    private String parseOpenAiResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            log.warn("OpenAI returned empty response");
            return OPENAI_FAILURE_MESSAGE;
        }

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).path("message");
                String content = message.path("content").asText();
                if (content != null && !content.trim().isEmpty()) {
                    return content;
                }
            }
            log.warn("OpenAI response did not contain expected content. responseSnippet={}", abbreviateForLog(response));
            return OPENAI_FAILURE_MESSAGE;
        } catch (Exception e) {
            log.warn("Failed to parse OpenAI response. responseSnippet={}", abbreviateForLog(response), e);
            return OPENAI_FAILURE_MESSAGE;
        }
    }

    private String abbreviateForLog(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        if (normalized.length() <= MAX_LOG_RESPONSE_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_LOG_RESPONSE_LENGTH) + "...";
    }

    @Override
    public boolean isAvailable() {
        return aiConfig.getOpenai().getApiKey() != null
                && !aiConfig.getOpenai().getApiKey().isEmpty();
    }

    @FunctionalInterface
    interface RetrySleeper {
        void sleep(long millis) throws InterruptedException;
    }
}
