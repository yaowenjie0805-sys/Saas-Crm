package com.yao.crm.controller;

import com.yao.crm.config.AiConfig;
import com.yao.crm.service.AiContentGenerationService;
import com.yao.crm.service.AiLeadClassificationService;
import com.yao.crm.service.AiSalesForecastService;
import com.yao.crm.service.I18nService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.util.Map;

import static com.yao.crm.support.TestTenant.TENANT_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class V1AiControllerTest {

    private static final int FOLLOW_UP_INTERACTION_DETAILS_MAX_LENGTH = 4000;

    private AiContentGenerationService aiContentGenerationService;
    private AiLeadClassificationService aiLeadClassificationService;
    private AiSalesForecastService aiSalesForecastService;
    private AiConfig aiConfig;
    private V1AiController controller;

    @BeforeEach
    void setUp() {
        aiContentGenerationService = mock(AiContentGenerationService.class);
        aiLeadClassificationService = mock(AiLeadClassificationService.class);
        aiSalesForecastService = mock(AiSalesForecastService.class);
        aiConfig = new AiConfig();
        aiConfig.getOpenai().setModel("gpt-4o");
        aiConfig.getAnthropic().setModel("claude-3-5-sonnet");
        I18nService i18nService = mock(I18nService.class);
        when(i18nService.msg(any(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));

        controller = new V1AiController(
                aiContentGenerationService,
                aiLeadClassificationService,
                aiSalesForecastService,
                aiConfig,
                i18nService
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void statusShouldReturnAvailability() {
        MockHttpServletRequest request = authedRequest("ANALYST");
        when(aiContentGenerationService.isAvailable()).thenReturn(true);

        ResponseEntity<?> response = controller.status(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("ai_status_loaded", body.get("code"));
        assertEquals(Boolean.TRUE, body.get("available"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void configShouldReturnAvailableModelsAndDefaultModel() {
        MockHttpServletRequest request = authedRequest("ANALYST");

        ResponseEntity<?> response = controller.config(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("ai_config_loaded", body.get("code"));
        assertEquals("gpt-4o", body.get("defaultModel"));
        assertEquals(Boolean.TRUE, body.get("canOverride"));
        assertEquals(Boolean.TRUE, body.get("supportsCustomConnection"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void statusShouldRejectForbiddenRole() {
        MockHttpServletRequest request = authedRequest("GUEST");

        ResponseEntity<?> response = controller.status(request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("forbidden", body.get("code"));
        verifyNoInteractions(aiContentGenerationService, aiLeadClassificationService, aiSalesForecastService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void followUpSummaryShouldRejectBlankInteractionDetails() {
        MockHttpServletRequest request = authedRequest("SALES");
        V1AiController.FollowUpSummaryRequest payload = new V1AiController.FollowUpSummaryRequest();
        payload.setCustomerName("Acme");
        payload.setInteractionDetails("   ");
        payload.setChannel("phone");

        ResponseEntity<?> response = controller.followUpSummary(request, payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("interaction_details_required", body.get("code"));
        verifyNoInteractions(aiContentGenerationService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void followUpSummaryShouldRejectTooLongInteractionDetails() {
        MockHttpServletRequest request = authedRequest("SALES");
        V1AiController.FollowUpSummaryRequest payload = new V1AiController.FollowUpSummaryRequest();
        payload.setCustomerName("Acme");
        payload.setInteractionDetails("a".repeat(FOLLOW_UP_INTERACTION_DETAILS_MAX_LENGTH + 1));
        payload.setChannel("phone");

        ResponseEntity<?> response = controller.followUpSummary(request, payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("interaction_details_too_long", body.get("code"));
        verifyNoInteractions(aiContentGenerationService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void followUpSummaryShouldReturnGeneratedSummary() {
        MockHttpServletRequest request = authedRequest("MANAGER");
        V1AiController.FollowUpSummaryRequest payload = new V1AiController.FollowUpSummaryRequest();
        payload.setCustomerName("  Acme  ");
        payload.setInteractionDetails("  Interested in annual plan  ");
        payload.setChannel("  phone  ");
        when(aiContentGenerationService.generateFollowUpSummary("Acme", "Interested in annual plan", "phone", null, null, null))
                .thenReturn("Summary");

        ResponseEntity<?> response = controller.followUpSummary(request, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("ai_follow_up_summary_generated", body.get("code"));
        assertEquals("Summary", body.get("summary"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void followUpSummaryShouldPassSelectedModelWhenProvided() {
        MockHttpServletRequest request = authedRequest("MANAGER");
        V1AiController.FollowUpSummaryRequest payload = new V1AiController.FollowUpSummaryRequest();
        payload.setCustomerName("Acme");
        payload.setInteractionDetails("Interested in annual plan");
        payload.setChannel("phone");
        payload.setModel("claude-3-5-sonnet");
        when(aiContentGenerationService.generateFollowUpSummary("Acme", "Interested in annual plan", "phone", "claude-3-5-sonnet", null, null))
                .thenReturn("Summary");

        ResponseEntity<?> response = controller.followUpSummary(request, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Summary", body.get("summary"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void followUpSummaryShouldAllowOptionalCustomerAndChannel() {
        MockHttpServletRequest request = authedRequest("MANAGER");
        V1AiController.FollowUpSummaryRequest payload = new V1AiController.FollowUpSummaryRequest();
        payload.setInteractionDetails("  Interested in annual plan  ");
        when(aiContentGenerationService.generateFollowUpSummary("Unknown Customer", "Interested in annual plan", "Unknown Channel", null, null, null))
                .thenReturn("Summary");

        ResponseEntity<?> response = controller.followUpSummary(request, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("ai_follow_up_summary_generated", body.get("code"));
        assertEquals("Summary", body.get("summary"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void followUpSummaryShouldRejectNullPayload() {
        MockHttpServletRequest request = authedRequest("MANAGER");

        ResponseEntity<?> response = controller.followUpSummary(request, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("bad_request", body.get("code"));
        verifyNoInteractions(aiContentGenerationService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void followUpSummaryShouldReturnServiceUnavailableWhenAiServiceThrows() {
        MockHttpServletRequest request = authedRequest("MANAGER");
        V1AiController.FollowUpSummaryRequest payload = new V1AiController.FollowUpSummaryRequest();
        payload.setCustomerName("Acme");
        payload.setInteractionDetails("Interested in annual plan");
        payload.setChannel("phone");
        when(aiContentGenerationService.generateFollowUpSummary(anyString(), anyString(), anyString(), any(), any(), any()))
                .thenThrow(new RuntimeException("downstream timeout"));

        ResponseEntity<?> response = controller.followUpSummary(request, payload);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("ai_service_unavailable", body.get("code"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void followUpSummaryShouldReturnServiceUnavailableWhenAiResponseIsBlank() {
        MockHttpServletRequest request = authedRequest("MANAGER");
        V1AiController.FollowUpSummaryRequest payload = new V1AiController.FollowUpSummaryRequest();
        payload.setCustomerName("Acme");
        payload.setInteractionDetails("Interested in annual plan");
        payload.setChannel("phone");
        when(aiContentGenerationService.generateFollowUpSummary(anyString(), anyString(), anyString(), any(), any(), any()))
                .thenReturn("   ");

        ResponseEntity<?> response = controller.followUpSummary(request, payload);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("ai_service_unavailable", body.get("code"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void followUpSummaryShouldReturnServiceUnavailableWhenAiReturnsNotConfiguredMessage() {
        MockHttpServletRequest request = authedRequest("MANAGER");
        V1AiController.FollowUpSummaryRequest payload = new V1AiController.FollowUpSummaryRequest();
        payload.setCustomerName("Acme");
        payload.setInteractionDetails("Interested in annual plan");
        payload.setChannel("phone");
        when(aiContentGenerationService.generateFollowUpSummary(anyString(), anyString(), anyString(), any(), any(), any()))
                .thenReturn("AI service is not configured. Please contact administrator.");

        ResponseEntity<?> response = controller.followUpSummary(request, payload);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("ai_service_unavailable", body.get("code"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void followUpSummaryShouldPassCustomConnectionWhenProvided() {
        MockHttpServletRequest request = authedRequest("MANAGER");
        V1AiController.FollowUpSummaryRequest payload = new V1AiController.FollowUpSummaryRequest();
        payload.setCustomerName("Acme");
        payload.setInteractionDetails("Interested in annual plan");
        payload.setChannel("phone");
        payload.setModel("gpt-4o");
        payload.setBaseUrl("http://localhost:11434/v1");
        payload.setApiKey("sk-local");
        when(aiContentGenerationService.generateFollowUpSummary(
                "Acme",
                "Interested in annual plan",
                "phone",
                "gpt-4o",
                "http://localhost:11434/v1",
                "sk-local"))
                .thenReturn("Summary");

        ResponseEntity<?> response = controller.followUpSummary(request, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Summary", body.get("summary"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void commentReplyShouldReturnGeneratedReply() {
        MockHttpServletRequest request = authedRequest("ADMIN");
        V1AiController.CommentReplyRequest payload = new V1AiController.CommentReplyRequest();
        payload.setOriginalComment("  Product is too expensive  ");
        payload.setContext("  Loyal customer  ");
        when(aiContentGenerationService.generateCommentReply("Product is too expensive", "Loyal customer", null, null, null))
                .thenReturn("Reply");

        ResponseEntity<?> response = controller.commentReply(request, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("ai_comment_reply_generated", body.get("code"));
        assertEquals("Reply", body.get("reply"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void commentReplyShouldPassCustomConnectionWhenProvided() {
        MockHttpServletRequest request = authedRequest("ADMIN");
        V1AiController.CommentReplyRequest payload = new V1AiController.CommentReplyRequest();
        payload.setOriginalComment("Product is too expensive");
        payload.setContext("Loyal customer");
        payload.setModel("gpt-4o-mini");
        payload.setBaseUrl("http://localhost:11434/v1");
        payload.setApiKey("sk-local");
        when(aiContentGenerationService.generateCommentReply(
                "Product is too expensive",
                "Loyal customer",
                "gpt-4o-mini",
                "http://localhost:11434/v1",
                "sk-local"))
                .thenReturn("Reply");

        ResponseEntity<?> response = controller.commentReply(request, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Reply", body.get("reply"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void commentReplyShouldRejectBlankContext() {
        MockHttpServletRequest request = authedRequest("ADMIN");
        V1AiController.CommentReplyRequest payload = new V1AiController.CommentReplyRequest();
        payload.setOriginalComment("question");
        payload.setContext("   ");

        ResponseEntity<?> response = controller.commentReply(request, payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("context_required", body.get("code"));
        verifyNoInteractions(aiContentGenerationService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void marketingEmailShouldReturnGeneratedEmail() {
        MockHttpServletRequest request = authedRequest("ADMIN");
        V1AiController.MarketingEmailRequest payload = new V1AiController.MarketingEmailRequest();
        payload.setCustomerName("Acme");
        payload.setProductName("CRM Plus");
        payload.setCustomerInterest("Automation");
        when(aiContentGenerationService.generateMarketingEmail("Acme", "CRM Plus", "Automation", null, null, null))
                .thenReturn("Email");

        ResponseEntity<?> response = controller.marketingEmail(request, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("ai_marketing_email_generated", body.get("code"));
        assertEquals("Email", body.get("email"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void marketingEmailShouldPassCustomConnectionWhenProvided() {
        MockHttpServletRequest request = authedRequest("ADMIN");
        V1AiController.MarketingEmailRequest payload = new V1AiController.MarketingEmailRequest();
        payload.setCustomerName("Acme");
        payload.setProductName("CRM Plus");
        payload.setCustomerInterest("Automation");
        payload.setModel("gpt-4o-mini");
        payload.setBaseUrl("http://localhost:11434/v1");
        payload.setApiKey("sk-local");
        when(aiContentGenerationService.generateMarketingEmail(
                "Acme",
                "CRM Plus",
                "Automation",
                "gpt-4o-mini",
                "http://localhost:11434/v1",
                "sk-local"))
                .thenReturn("Email");

        ResponseEntity<?> response = controller.marketingEmail(request, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Email", body.get("email"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void leadQualityAssessmentShouldReturnAssessment() {
        MockHttpServletRequest request = authedRequest("ANALYST");
        V1AiController.LeadQualityAssessmentRequest payload = new V1AiController.LeadQualityAssessmentRequest();
        payload.setLeadName("Neo");
        payload.setCompany("Matrix");
        payload.setPhone("13800000000");
        payload.setEmail("neo@matrix.com");
        payload.setDescription("Looking for migration");
        Map<String, Object> assessment = Map.of("overallScore", 82.5);
        when(aiLeadClassificationService.assessLeadQuality("Neo", "Matrix", "13800000000", "neo@matrix.com", "Looking for migration"))
                .thenReturn(assessment);

        ResponseEntity<?> response = controller.leadQualityAssessment(request, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("ai_lead_quality_assessed", body.get("code"));
        assertEquals(assessment, body.get("assessment"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void winProbabilityShouldReturnCalculatedValue() {
        MockHttpServletRequest request = authedRequest("SALES");
        V1AiController.WinProbabilityRequest payload = new V1AiController.WinProbabilityRequest();
        payload.setOpportunityName("Q2 Renewal");
        payload.setStage("NEGOTIATION");
        payload.setAmount("120000");
        payload.setDaysInStage("12");
        payload.setCompetitorInfo("Competitor A");
        when(aiSalesForecastService.calculateWinProbability(
                eq("Q2 Renewal"),
                eq("NEGOTIATION"),
                eq(new BigDecimal("120000")),
                eq(12),
                eq("Competitor A")))
                .thenReturn(68.3d);

        ResponseEntity<?> response = controller.winProbability(request, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("ai_win_probability_calculated", body.get("code"));
        assertEquals(68.3d, body.get("winProbability"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void salesAdviceShouldReturnGeneratedAdvice() {
        MockHttpServletRequest request = authedRequest("MANAGER");
        V1AiController.SalesAdviceRequest payload = new V1AiController.SalesAdviceRequest();
        payload.setOpportunityName("Q2 Renewal");
        payload.setStage("PROPOSAL");
        payload.setCustomerName("Acme");
        payload.setLastActivity("Sent proposal");
        when(aiSalesForecastService.generateSalesAdvice("Q2 Renewal", "PROPOSAL", "Acme", "Sent proposal", null, null, null))
                .thenReturn("Advice");

        ResponseEntity<?> response = controller.salesAdvice(request, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("ai_sales_advice_generated", body.get("code"));
        assertEquals("Advice", body.get("advice"));
        verify(aiSalesForecastService).generateSalesAdvice("Q2 Renewal", "PROPOSAL", "Acme", "Sent proposal", null, null, null);
    }

    @Test
    @SuppressWarnings("unchecked")
    void salesAdviceShouldPassCustomConnectionWhenProvided() {
        MockHttpServletRequest request = authedRequest("MANAGER");
        V1AiController.SalesAdviceRequest payload = new V1AiController.SalesAdviceRequest();
        payload.setOpportunityName("Q2 Renewal");
        payload.setStage("PROPOSAL");
        payload.setCustomerName("Acme");
        payload.setLastActivity("Sent proposal");
        payload.setModel("gpt-4o-mini");
        payload.setBaseUrl("http://localhost:11434/v1");
        payload.setApiKey("sk-local");
        when(aiSalesForecastService.generateSalesAdvice(
                "Q2 Renewal",
                "PROPOSAL",
                "Acme",
                "Sent proposal",
                "gpt-4o-mini",
                "http://localhost:11434/v1",
                "sk-local"))
                .thenReturn("Advice");

        ResponseEntity<?> response = controller.salesAdvice(request, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Advice", body.get("advice"));
    }

    private MockHttpServletRequest authedRequest(String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authRole", role);
        request.setAttribute("authUsername", "alice");
        request.setAttribute("authTenantId", TENANT_TEST);
        return request;
    }
}
