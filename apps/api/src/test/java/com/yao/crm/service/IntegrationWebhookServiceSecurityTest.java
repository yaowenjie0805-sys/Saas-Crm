package com.yao.crm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.entity.NotificationChannel;
import com.yao.crm.repository.NotificationChannelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class IntegrationWebhookServiceSecurityTest {

    @Mock
    private NotificationChannelRepository notificationChannelRepository;

    @Test
    void sanitizeWebhookUrlShouldRejectNonHttps() {
        IntegrationWebhookService service = newService("");

        assertEquals("", service.sanitizeWebhookUrl("http://example.com/webhook"));
    }

    @Test
    void sanitizeBaseUrlShouldRejectNonHttps() {
        IntegrationWebhookService service = newService("");

        assertEquals("", service.sanitizeBaseUrl("http://open.feishu.cn"));
    }

    @Test
    void sanitizeBaseUrlShouldRejectLocalhost() {
        IntegrationWebhookService service = newService("");

        assertEquals("", service.sanitizeBaseUrl("https://localhost/open-apis"));
    }

    @Test
    void sanitizeWebhookUrlShouldRejectLocalOrPrivateHosts() {
        IntegrationWebhookService service = newService("");

        assertEquals("", service.sanitizeWebhookUrl("https://127.0.0.1/webhook"));
        assertEquals("", service.sanitizeWebhookUrl("https://192.168.1.8/webhook"));
        assertEquals("", service.sanitizeWebhookUrl("https://localhost/webhook"));
    }

    @Test
    void sanitizeWebhookUrlShouldRejectIpv6Loopback() {
        IntegrationWebhookService service = newService("");

        assertEquals("", service.sanitizeWebhookUrl("https://[::1]/webhook"));
        assertEquals("", service.sanitizeWebhookUrl("https://[0:0:0:0:0:0:0:1]/webhook"));
    }

    @Test
    void sanitizeWebhookUrlShouldRespectAllowedHostSuffixes() {
        IntegrationWebhookService service = newService("hooks.example.com open.feishu.cn");

        assertEquals("", service.sanitizeWebhookUrl("https://api.example.com/webhook"));
        assertEquals("https://hooks.example.com/webhook", service.sanitizeWebhookUrl("https://hooks.example.com/webhook"));
        assertEquals("https://open.feishu.cn/open-apis/im/v1/messages", service.sanitizeWebhookUrl("https://open.feishu.cn/open-apis/im/v1/messages"));
    }

    @Test
    void sanitizeWebhookUrlShouldAllowSubdomainWhenSuffixIsInAllowlist() {
        IntegrationWebhookService service = newService("example.com");

        assertEquals("https://hooks.example.com/webhook", service.sanitizeWebhookUrl("https://hooks.example.com/webhook"));
        assertEquals("", service.sanitizeWebhookUrl("https://example.org/webhook"));
    }

    @Test
    void sanitizeWebhookUrlShouldAllowPublicHttpsWhenAllowlistIsEmpty() {
        IntegrationWebhookService service = newService("");

        assertEquals("https://hooks.example.com/webhook", service.sanitizeWebhookUrl("https://hooks.example.com/webhook"));
    }

    @Test
    void sanitizeWebhookUrlShouldReturnEmptyForInvalidUrlString() {
        IntegrationWebhookService service = newService("");

        assertEquals("", service.sanitizeWebhookUrl("https://bad host/webhook"));
        assertEquals("", service.sanitizeWebhookUrl("://missing-scheme"));
    }

    @Test
    void sendMessageShouldReturnFalseWhenTenantWebhookUrlIsMaliciousHttp() {
        IntegrationWebhookService service = newService("");
        String tenantId = "tenant-http";
        NotificationChannel channel = enabledChannel("{\"webhookUrl\":\"http://evil.example.com/wh\"}");
        when(notificationChannelRepository.findTopByTenantAndChannelType(tenantId, "WECHAT_WORK"))
                .thenReturn(Collections.singletonList(channel));

        boolean success = service.sendMessage("WECOM", tenantId, "title", "content", "u1");

        assertFalse(success);
        verify(notificationChannelRepository).findTopByTenantAndChannelType(tenantId, "WECHAT_WORK");
    }

    @Test
    void sendMessageShouldReturnFalseWhenTenantWebhookUrlIsLoopbackHttps() {
        IntegrationWebhookService service = newService("");
        String tenantId = "tenant-loopback";
        NotificationChannel channel = enabledChannel("{\"webhookUrl\":\"https://127.0.0.1/wh\"}");
        when(notificationChannelRepository.findTopByTenantAndChannelType(tenantId, "WECHAT_WORK"))
                .thenReturn(Collections.singletonList(channel));

        boolean success = service.sendMessage("WECOM", tenantId, "title", "content", "u2");

        assertFalse(success);
        verify(notificationChannelRepository).findTopByTenantAndChannelType(tenantId, "WECHAT_WORK");
    }

    @Test
    void sendMessageDetailedShouldRetryRetryableResponsesAndExposeRetryableResult() {
        IntegrationWebhookService service = newService("");
        String tenantId = "tenant-retryable";
        NotificationChannel channel = enabledChannel("{\"webhookUrl\":\"https://hooks.example.com/wh\"}");
        when(notificationChannelRepository.findTopByTenantAndChannelType(tenantId, "WECHAT_WORK"))
                .thenReturn(Collections.singletonList(channel));

        RestTemplate restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);

        ResponseEntity<String> retryableResponse = new ResponseEntity<String>("temporary", HttpStatus.INTERNAL_SERVER_ERROR);
        ResponseEntity<String> successResponse = new ResponseEntity<String>("ok", HttpStatus.OK);
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), org.mockito.ArgumentMatchers.eq(String.class)))
                .thenReturn(retryableResponse, successResponse);

        IntegrationWebhookService.DispatchResult result = service.sendMessageDetailed("WECOM", tenantId, "title", "content", "u3");

        assertTrue(result.isSuccess());
        assertTrue(result.isRetryable());
        assertEquals(2, result.getAttempts());
        verify(restTemplate, times(2)).postForEntity(any(String.class), any(HttpEntity.class), org.mockito.ArgumentMatchers.eq(String.class));
    }

    private IntegrationWebhookService newService(String allowedHostSuffixes) {
        return new IntegrationWebhookService(
                notificationChannelRepository,
                new ObjectMapper(),
                new RestTemplateBuilder(),
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "chat_id",
                "https://open.feishu.cn",
                allowedHostSuffixes
        );
    }

    private NotificationChannel enabledChannel(String configJson) {
        NotificationChannel channel = new NotificationChannel();
        channel.setEnabled(true);
        channel.setConfigJson(configJson);
        return channel;
    }
}


