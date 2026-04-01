package com.yao.crm.controller;

import com.yao.crm.service.AiContentGenerationService;
import com.yao.crm.service.AiLeadClassificationService;
import com.yao.crm.service.AiSalesForecastService;
import com.yao.crm.service.I18nService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/ai")
public class V1AiController extends BaseApiController {

    private static final Logger log = LoggerFactory.getLogger(V1AiController.class);
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final String AI_SERVICE_UNAVAILABLE_CODE = "ai_service_unavailable";
    private static final int FOLLOW_UP_INTERACTION_DETAILS_MAX_LENGTH = 4000;

    private final AiContentGenerationService aiContentGenerationService;
    private final AiLeadClassificationService aiLeadClassificationService;
    private final AiSalesForecastService aiSalesForecastService;

    public V1AiController(AiContentGenerationService aiContentGenerationService,
                          AiLeadClassificationService aiLeadClassificationService,
                          AiSalesForecastService aiSalesForecastService,
                          I18nService i18nService) {
        super(i18nService);
        this.aiContentGenerationService = aiContentGenerationService;
        this.aiLeadClassificationService = aiLeadClassificationService;
        this.aiSalesForecastService = aiSalesForecastService;
    }

    @GetMapping("/status")
    public ResponseEntity<?> status(HttpServletRequest request) {
        if (!hasAiAccess(request)) {
            return forbidden(request);
        }

        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("available", aiContentGenerationService.isAvailable());
        return ResponseEntity.ok(successWithFields(request, "ai_status_loaded", out));
    }

    @PostMapping("/followUpSummary")
    public ResponseEntity<?> followUpSummary(HttpServletRequest request,
                                             @RequestBody(required = false) FollowUpSummaryRequest payload) {
        if (!hasAiAccess(request)) {
            return forbidden(request);
        }
        if (payload == null) {
            return badRequest(request, "bad_request");
        }
        String validationCode = firstValidationCode(payload, "interactionDetails");
        if (!isBlank(validationCode)) return badRequest(request, validationCode);

        String customerName = normalize(payload.getCustomerName());
        String interactionDetails = normalize(payload.getInteractionDetails());
        if (interactionDetails.length() > FOLLOW_UP_INTERACTION_DETAILS_MAX_LENGTH) {
            return badRequest(request, "interaction_details_too_long");
        }
        String channel = normalize(payload.getChannel());
        final String finalCustomerName = isBlank(customerName) ? "Unknown Customer" : customerName;
        final String finalChannel = isBlank(channel) ? "Unknown Channel" : channel;

        String summary = invokeAiText("follow_up_summary", new AiTextOperation() {
            @Override
            public String execute() {
                return aiContentGenerationService.generateFollowUpSummary(finalCustomerName, interactionDetails, finalChannel);
            }
        });
        if (summary == null) {
            return aiServiceUnavailable(request, "follow_up_summary");
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("summary", summary);
        return ResponseEntity.ok(successWithFields(request, "ai_follow_up_summary_generated", out));
    }

    @PostMapping("/commentReply")
    public ResponseEntity<?> commentReply(HttpServletRequest request,
                                          @RequestBody(required = false) CommentReplyRequest payload) {
        if (!hasAiAccess(request)) {
            return forbidden(request);
        }
        if (payload == null) {
            return badRequest(request, "bad_request");
        }
        String validationCode = firstValidationCode(payload, "originalComment", "context");
        if (!isBlank(validationCode)) return badRequest(request, validationCode);

        String originalComment = normalize(payload.getOriginalComment());
        String context = normalize(payload.getContext());

        String reply = invokeAiText("comment_reply", new AiTextOperation() {
            @Override
            public String execute() {
                return aiContentGenerationService.generateCommentReply(originalComment, context);
            }
        });
        if (reply == null) {
            return aiServiceUnavailable(request, "comment_reply");
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("reply", reply);
        return ResponseEntity.ok(successWithFields(request, "ai_comment_reply_generated", out));
    }

    @PostMapping("/marketingEmail")
    public ResponseEntity<?> marketingEmail(HttpServletRequest request,
                                            @RequestBody(required = false) MarketingEmailRequest payload) {
        if (!hasAiAccess(request)) {
            return forbidden(request);
        }
        if (payload == null) {
            return badRequest(request, "bad_request");
        }
        String validationCode = firstValidationCode(payload, "customerName", "productName", "customerInterest");
        if (!isBlank(validationCode)) return badRequest(request, validationCode);

        String customerName = normalize(payload.getCustomerName());
        String productName = normalize(payload.getProductName());
        String customerInterest = normalize(payload.getCustomerInterest());

        String email = invokeAiText("marketing_email", new AiTextOperation() {
            @Override
            public String execute() {
                return aiContentGenerationService.generateMarketingEmail(customerName, productName, customerInterest);
            }
        });
        if (email == null) {
            return aiServiceUnavailable(request, "marketing_email");
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("email", email);
        return ResponseEntity.ok(successWithFields(request, "ai_marketing_email_generated", out));
    }

    @PostMapping("/leadQualityAssessment")
    public ResponseEntity<?> leadQualityAssessment(HttpServletRequest request,
                                                   @RequestBody(required = false) LeadQualityAssessmentRequest payload) {
        if (!hasAiAccess(request)) {
            return forbidden(request);
        }
        if (payload == null) {
            return badRequest(request, "bad_request");
        }
        String validationCode = firstValidationCode(payload, "leadName", "company");
        if (!isBlank(validationCode)) return badRequest(request, validationCode);

        String leadName = normalize(payload.getLeadName());
        String company = normalize(payload.getCompany());
        String phone = normalize(payload.getPhone());
        String email = normalize(payload.getEmail());
        String description = normalize(payload.getDescription());

        Map<String, Object> assessment;
        try {
            assessment = aiLeadClassificationService.assessLeadQuality(
                    leadName,
                    company,
                    phone,
                    email,
                    description
            );
        } catch (Exception ex) {
            log.warn("AI operation {} failed", "lead_quality_assessment", ex);
            return aiServiceUnavailable(request, "lead_quality_assessment");
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("assessment", assessment);
        return ResponseEntity.ok(successWithFields(request, "ai_lead_quality_assessed", out));
    }

    @PostMapping("/winProbability")
    public ResponseEntity<?> winProbability(HttpServletRequest request,
                                            @RequestBody(required = false) WinProbabilityRequest payload) {
        if (!hasAiAccess(request)) {
            return forbidden(request);
        }
        if (payload == null) {
            return badRequest(request, "bad_request");
        }
        String validationCode = firstValidationCode(payload, "opportunityName", "stage");
        if (!isBlank(validationCode)) return badRequest(request, validationCode);

        String opportunityName = normalize(payload.getOpportunityName());
        String stage = normalize(payload.getStage());

        BigDecimal amount = parseAmount(payload.getAmount());
        if (payload.getAmount() != null && amount == null) {
            return badRequest(request, "amount_invalid");
        }

        Integer daysInStage = parseDaysInStage(payload.getDaysInStage());
        if (payload.getDaysInStage() != null && daysInStage == null) {
            return badRequest(request, "days_in_stage_invalid");
        }

        String competitorInfo = normalize(payload.getCompetitorInfo());
        double winProbability;
        try {
            winProbability = aiSalesForecastService.calculateWinProbability(
                    opportunityName,
                    stage,
                    amount,
                    daysInStage == null ? 0 : daysInStage,
                    competitorInfo
            );
        } catch (Exception ex) {
            log.warn("AI operation {} failed", "win_probability", ex);
            return aiServiceUnavailable(request, "win_probability");
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("winProbability", winProbability);
        return ResponseEntity.ok(successWithFields(request, "ai_win_probability_calculated", out));
    }

    @PostMapping("/salesAdvice")
    public ResponseEntity<?> salesAdvice(HttpServletRequest request,
                                         @RequestBody(required = false) SalesAdviceRequest payload) {
        if (!hasAiAccess(request)) {
            return forbidden(request);
        }
        if (payload == null) {
            return badRequest(request, "bad_request");
        }
        String validationCode = firstValidationCode(payload, "opportunityName", "stage", "customerName", "lastActivity");
        if (!isBlank(validationCode)) return badRequest(request, validationCode);

        String opportunityName = normalize(payload.getOpportunityName());
        String stage = normalize(payload.getStage());
        String customerName = normalize(payload.getCustomerName());
        String lastActivity = normalize(payload.getLastActivity());

        String advice = invokeAiText("sales_advice", new AiTextOperation() {
            @Override
            public String execute() {
                return aiSalesForecastService.generateSalesAdvice(opportunityName, stage, customerName, lastActivity);
            }
        });
        if (advice == null) {
            return aiServiceUnavailable(request, "sales_advice");
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("advice", advice);
        return ResponseEntity.ok(successWithFields(request, "ai_sales_advice_generated", out));
    }

    private boolean hasAiAccess(HttpServletRequest request) {
        return hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST");
    }

    private ResponseEntity<?> forbidden(HttpServletRequest request) {
        return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
    }

    private ResponseEntity<?> badRequest(HttpServletRequest request, String key) {
        String code = normalizeCode(key, "bad_request");
        return ResponseEntity.badRequest().body(errorBody(request, code, msg(request, code), null));
    }

    private ResponseEntity<?> aiServiceUnavailable(HttpServletRequest request, String operation) {
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("operation", operation);
        return ResponseEntity.status(503).body(errorBody(
                request,
                AI_SERVICE_UNAVAILABLE_CODE,
                msg(request, AI_SERVICE_UNAVAILABLE_CODE),
                details
        ));
    }

    private String invokeAiText(String operation, AiTextOperation operationCall) {
        try {
            String result = operationCall.execute();
            if (isBlank(result)) {
                log.warn("AI operation {} returned blank response", operation);
                return null;
            }
            return result;
        } catch (Exception ex) {
            log.warn("AI operation {} failed", operation, ex);
            return null;
        }
    }

    private interface AiTextOperation {
        String execute();
    }

    private String firstValidationCode(Object payload, String... fields) {
        for (String field : fields) {
            Set<ConstraintViolation<Object>> violations = VALIDATOR.validateProperty(payload, field);
            if (!violations.isEmpty()) {
                ConstraintViolation<Object> violation = violations.iterator().next();
                return normalizeCode(violation.getMessage(), "bad_request");
            }
        }
        return null;
    }

    private String normalize(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private BigDecimal parseAmount(Object value) {
        String normalized = normalize(value);
        if (isBlank(normalized)) {
            return null;
        }
        try {
            return new BigDecimal(normalized);
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer parseDaysInStage(Object value) {
        String normalized = normalize(value);
        if (isBlank(normalized)) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(normalized);
            return parsed < 0 ? null : parsed;
        } catch (Exception ex) {
            return null;
        }
    }

    public static class FollowUpSummaryRequest {
        private String customerName;

        @NotBlank(message = "interaction_details_required")
        private String interactionDetails;

        private String channel;

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public String getInteractionDetails() {
            return interactionDetails;
        }

        public void setInteractionDetails(String interactionDetails) {
            this.interactionDetails = interactionDetails;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }
    }

    public static class CommentReplyRequest {
        @NotBlank(message = "original_comment_required")
        private String originalComment;

        @NotBlank(message = "context_required")
        private String context;

        public String getOriginalComment() {
            return originalComment;
        }

        public void setOriginalComment(String originalComment) {
            this.originalComment = originalComment;
        }

        public String getContext() {
            return context;
        }

        public void setContext(String context) {
            this.context = context;
        }
    }

    public static class MarketingEmailRequest {
        @NotBlank(message = "customer_name_required")
        private String customerName;

        @NotBlank(message = "product_name_required")
        private String productName;

        @NotBlank(message = "customer_interest_required")
        private String customerInterest;

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public String getCustomerInterest() {
            return customerInterest;
        }

        public void setCustomerInterest(String customerInterest) {
            this.customerInterest = customerInterest;
        }
    }

    public static class LeadQualityAssessmentRequest {
        @NotBlank(message = "lead_name_required")
        private String leadName;

        @NotBlank(message = "company_required")
        private String company;

        private String phone;
        private String email;
        private String description;

        public String getLeadName() {
            return leadName;
        }

        public void setLeadName(String leadName) {
            this.leadName = leadName;
        }

        public String getCompany() {
            return company;
        }

        public void setCompany(String company) {
            this.company = company;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class WinProbabilityRequest {
        @NotBlank(message = "opportunity_name_required")
        private String opportunityName;

        @NotBlank(message = "stage_required")
        private String stage;

        private String amount;
        private String daysInStage;
        private String competitorInfo;

        public String getOpportunityName() {
            return opportunityName;
        }

        public void setOpportunityName(String opportunityName) {
            this.opportunityName = opportunityName;
        }

        public String getStage() {
            return stage;
        }

        public void setStage(String stage) {
            this.stage = stage;
        }

        public String getAmount() {
            return amount;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }

        public String getDaysInStage() {
            return daysInStage;
        }

        public void setDaysInStage(String daysInStage) {
            this.daysInStage = daysInStage;
        }

        public String getCompetitorInfo() {
            return competitorInfo;
        }

        public void setCompetitorInfo(String competitorInfo) {
            this.competitorInfo = competitorInfo;
        }
    }

    public static class SalesAdviceRequest {
        @NotBlank(message = "opportunity_name_required")
        private String opportunityName;

        @NotBlank(message = "stage_required")
        private String stage;

        @NotBlank(message = "customer_name_required")
        private String customerName;

        @NotBlank(message = "last_activity_required")
        private String lastActivity;

        public String getOpportunityName() {
            return opportunityName;
        }

        public void setOpportunityName(String opportunityName) {
            this.opportunityName = opportunityName;
        }

        public String getStage() {
            return stage;
        }

        public void setStage(String stage) {
            this.stage = stage;
        }

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public String getLastActivity() {
            return lastActivity;
        }

        public void setLastActivity(String lastActivity) {
            this.lastActivity = lastActivity;
        }
    }
}
