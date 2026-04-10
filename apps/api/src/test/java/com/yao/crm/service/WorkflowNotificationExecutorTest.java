package com.yao.crm.service;

import com.yao.crm.security.TraceIdInterceptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowNotificationExecutorTest {

    private final WorkflowNotificationExecutor executor = new WorkflowNotificationExecutor();
    private AutoCloseable mocks;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private IntegrationWebhookService integrationWebhookService;

    @BeforeEach
    void setUpMocks() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        RequestContextHolder.resetRequestAttributes();
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void execute_Email_ShouldBuildExpectedPayload() {
        Map<String, Object> config = new HashMap<>();
        config.put("to", "a@example.com");
        config.put("subject", "hello");

        Map<String, Object> output = executor.execute("EMAIL", config);

        assertEquals("EMAIL", output.get("channel"));
        assertEquals("a@example.com", output.get("recipient"));
        assertEquals("hello", output.get("subject"));
        assertNotNull(output.get("messageId"));
    }

    @Test
    void execute_EmailLowercaseType_ShouldBuildExpectedPayload() {
        Map<String, Object> config = new HashMap<>();
        config.put("to", "b@example.com");
        config.put("subject", "lowercase");

        Map<String, Object> output = executor.execute("email", config);

        assertEquals("EMAIL", output.get("channel"));
        assertEquals("b@example.com", output.get("recipient"));
        assertEquals("lowercase", output.get("subject"));
        assertNotNull(output.get("messageId"));
    }

    @Test
    void execute_WechatWork_ShouldBuildExpectedPayload() {
        Map<String, Object> config = new HashMap<>();
        config.put("agentId", "10001");
        config.put("userIds", "u1,u2");

        Map<String, Object> output = executor.execute("WECHAT_WORK", config);

        assertEquals("WECHAT_WORK", output.get("channel"));
        assertEquals("10001", output.get("agentId"));
        assertEquals("u1,u2", output.get("userIds"));
        assertNotNull(output.get("messageId"));
    }

    @Test
    void execute_DingTalk_ShouldBuildExpectedPayload() {
        Map<String, Object> config = new HashMap<>();
        config.put("webhook", "https://oapi.dingtalk.com/robot/send");

        Map<String, Object> output = executor.execute("DINGTALK", config);

        assertEquals("DINGTALK", output.get("channel"));
        assertEquals("https://oapi.dingtalk.com/robot/send", output.get("webhook"));
        assertNotNull(output.get("messageId"));
    }

    @Test
    void execute_InApp_ShouldBuildExpectedPayload() {
        Map<String, Object> config = new HashMap<>();
        config.put("title", "Approval needed");
        config.put("recipientUserId", "u100");

        Map<String, Object> output = executor.execute("IN_APP", config);

        assertEquals("IN_APP", output.get("channel"));
        assertEquals("Approval needed", output.get("title"));
        assertEquals("u100", output.get("recipientUserId"));
        assertNotNull(output.get("notificationId"));
    }

    @Test
    void execute_Sms_ShouldBuildExpectedPayload() {
        Map<String, Object> config = new HashMap<>();
        config.put("phoneNumber", "13800000000");

        Map<String, Object> output = executor.execute("SMS", config);

        assertEquals("SMS", output.get("channel"));
        assertEquals("13800000000", output.get("phoneNumber"));
        assertNotNull(output.get("messageId"));
    }

    @Test
    void execute_UnknownType_ShouldReturnEmptyOutput() {
        Map<String, Object> output = executor.execute("UNKNOWN", new HashMap<>());

        assertTrue(output.isEmpty());
    }

    @Test
    void sendNotificationShouldRecordRequestIdAndRetryableAuditFields() {
        NotificationDispatchService service = new NotificationDispatchService(auditLogService, integrationWebhookService, "WECOM");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(TraceIdInterceptor.TRACE_ID_ATTR, "trace-123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("customerName", "Acme");
        payload.put("stage", "Proposal");

        when(integrationWebhookService.sendEventDetailed(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(IntegrationWebhookService.DispatchResult.failure(3, 500, true));

        service.sendNotification("tenant-1", "workflow_event", payload);

        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).record(anyString(), anyString(), anyString(), anyString(), anyString(), detailsCaptor.capture(), anyString());
        String details = detailsCaptor.getValue();
        assertTrue(details.contains("requestId=trace-123"));
        assertTrue(details.contains("retryable=true"));
        assertTrue(details.contains("sent=false"));
        assertTrue(details.contains("eventScope=notification"));
        assertTrue(details.contains("customerName=Acme"));
    }
}
