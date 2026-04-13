package com.yao.crm.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.config.AiConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenAiServiceImplTest {

    @Test
    @DisplayName("shouldRetryOn429AndReturnContent_whenSecondAttemptSucceeds")
    void shouldRetryOn429AndReturnContent_whenSecondAttemptSucceeds() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        HttpClientErrorException tooManyRequests = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS,
                "rate limited",
                HttpHeaders.EMPTY,
                "{\"error\":\"rate_limited\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenThrow(tooManyRequests)
                .thenReturn(ResponseEntity.ok("{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}"));

        OpenAiServiceImpl service = new OpenAiServiceImpl(
                createAiConfig(),
                restTemplate,
                new ObjectMapper(),
                2,
                0,
                millis -> {
                }
        );

        String result = service.generateText("hello", Map.of("temperature", 0.3));

        assertEquals("ok", result);
        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));
    }

    @Test
    @DisplayName("shouldReturnGenericMessage_when5xxRetriesExhausted")
    void shouldReturnGenericMessage_when5xxRetriesExhausted() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        HttpServerErrorException serverError = HttpServerErrorException.create(
                HttpStatus.BAD_GATEWAY,
                "bad gateway",
                HttpHeaders.EMPTY,
                "{\"error\":\"upstream\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenThrow(serverError)
                .thenThrow(serverError)
                .thenThrow(serverError);

        OpenAiServiceImpl service = new OpenAiServiceImpl(
                createAiConfig(),
                restTemplate,
                new ObjectMapper(),
                2,
                0,
                millis -> {
                }
        );

        String result = service.generateText("hello");

        assertEquals("AI service is temporarily unavailable. Please try again later.", result);
        verify(restTemplate, times(3)).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));
    }

    @Test
    @DisplayName("shouldApplyConfiguredTimeouts_whenUsingPropertyConstructor")
    void shouldApplyConfiguredTimeouts_whenUsingPropertyConstructor() {
        OpenAiServiceImpl service = new OpenAiServiceImpl(createAiConfig(), new ObjectMapper(), new RestTemplateBuilder(), 1234, 5678, 2, 50);

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        SimpleClientHttpRequestFactory factory = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();

        assertEquals(1234, ReflectionTestUtils.getField(factory, "connectTimeout"));
        assertEquals(5678, ReflectionTestUtils.getField(factory, "readTimeout"));
    }

    @Test
    @DisplayName("shouldUseCustomBaseUrlAndApiKey_whenProvidedInOptions")
    void shouldUseCustomBaseUrlAndApiKey_whenProvidedInOptions() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}"));

        OpenAiServiceImpl service = new OpenAiServiceImpl(
                createAiConfig(),
                restTemplate,
                new ObjectMapper(),
                0,
                0,
                millis -> {
                }
        );

        String result = service.generateText(
                "hello",
                Map.of(
                        "model", "gpt-4o-mini",
                        "base_url", "http://localhost:11434/v1",
                        "api_key", "sk-local"
                )
        );

        assertEquals("ok", result);
        verify(restTemplate).exchange(contains("http://localhost:11434/v1/chat/completions"), eq(HttpMethod.POST), any(), eq(String.class));
    }

    @Test
    @DisplayName("shouldNormalizeCustomBaseUrlWithTrailingSlash_whenProvidedInOptions")
    void shouldNormalizeCustomBaseUrlWithTrailingSlash_whenProvidedInOptions() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}"));

        OpenAiServiceImpl service = new OpenAiServiceImpl(
                createAiConfig(),
                restTemplate,
                new ObjectMapper(),
                0,
                0,
                millis -> {
                }
        );

        String result = service.generateText(
                "hello",
                Map.of(
                        "base_url", "http://localhost:11434/v1/",
                        "api_key", "sk-local"
                )
        );

        assertEquals("ok", result);
        verify(restTemplate).exchange(eq("http://localhost:11434/v1/chat/completions"), eq(HttpMethod.POST), any(), eq(String.class));
    }

    private AiConfig createAiConfig() {
        AiConfig aiConfig = new AiConfig();
        AiConfig.OpenAi openAi = new AiConfig.OpenAi();
        openAi.setApiKey("test-api-key");
        openAi.setBaseUrl("https://api.openai.com");
        openAi.setModel("gpt-4o");
        aiConfig.setOpenai(openAi);
        return aiConfig;
    }
}



