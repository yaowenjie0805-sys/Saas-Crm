package com.yao.crm.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class ValueNormalizerService {

    private static final Map<String, String> CUSTOMER_STATUS_MAP = new HashMap<String, String>() { {
        put("ACTIVE", "Active");
        put("PENDING", "Pending");
        put("INACTIVE", "Inactive");
        put("NEGOTIATING", "Active");
        put("QUOTATION_PENDING", "Pending");
        put("FIRST_CONTACT", "Pending");
        put("PROPOSAL_REVIEW", "Active");
    } };

    private static final Map<String, String> OPPORTUNITY_STAGE_MAP = new HashMap<String, String>() { {
        put("LEAD", "Lead");
        put("QUALIFIED", "Qualified");
        put("PROPOSAL", "Proposal");
        put("NEGOTIATION", "Negotiation");
        put("CLOSED_WON", "Closed Won");
        put("CLOSED_LOST", "Closed Lost");
        put("LEAD_COLLECTION", "Lead");
        put("INITIAL_CONTACT", "Lead");
        put("PROPOSAL_FOLLOW_UP", "Proposal");
        put("BUSINESS_NEGOTIATION", "Negotiation");
    } };

    private static final Map<String, String> CONTRACT_STATUS_MAP = new HashMap<String, String>() { {
        put("DRAFT", "Draft");
        put("SIGNED", "Signed");
    } };

    private static final Map<String, String> PAYMENT_STATUS_MAP = new HashMap<String, String>() { {
        put("PENDING", "Pending");
        put("RECEIVED", "Received");
        put("OVERDUE", "Overdue");
    } };

    private static final Map<String, String> PAYMENT_METHOD_MAP = new HashMap<String, String>() { {
        put("BANK", "Bank");
        put("BANK_TRANSFER", "Bank");
        put("TRANSFER", "Transfer");
        put("CASH", "Cash");
        put("CARD", "Card");
    } };

    private static final Map<String, String> FOLLOWUP_CHANNEL_MAP = new HashMap<String, String>() { {
        put("PHONE", "Phone");
        put("EMAIL", "Email");
        put("WECHAT", "WeChat");
        put("VISIT", "Visit");
        put("MEETING", "Meeting");
    } };

    public String normalizeContractStatus(String value) {
        return normalizeByMap(value, CONTRACT_STATUS_MAP);
    }

    public String normalizeCustomerStatus(String value) {
        return normalizeByMap(value, CUSTOMER_STATUS_MAP);
    }

    public String normalizeOpportunityStage(String value) {
        return normalizeByMap(value, OPPORTUNITY_STAGE_MAP);
    }

    public String normalizePaymentStatus(String value) {
        return normalizeByMap(value, PAYMENT_STATUS_MAP);
    }

    public String normalizePaymentMethod(String value) {
        return normalizeByMap(value, PAYMENT_METHOD_MAP);
    }

    public String normalizeFollowUpChannel(String value) {
        return normalizeByMap(value, FOLLOWUP_CHANNEL_MAP);
    }

    public String normalizeQueryToken(String value) {
        if (value == null) return "";
        return value.trim();
    }

    public boolean isValidCustomerStatus(String value) {
        if (value == null) return false;
        String normalized = normalizeCustomerStatus(value);
        return "Active".equals(normalized) || "Pending".equals(normalized) || "Inactive".equals(normalized);
    }

    public boolean isValidOpportunityStage(String value) {
        if (value == null) return false;
        String normalized = normalizeOpportunityStage(value);
        return "Lead".equals(normalized)
                || "Qualified".equals(normalized)
                || "Proposal".equals(normalized)
                || "Negotiation".equals(normalized)
                || "Closed Won".equals(normalized)
                || "Closed Lost".equals(normalized);
    }

    public boolean isValidContractStatus(String value) {
        if (value == null) return false;
        String normalized = normalizeContractStatus(value);
        return "Draft".equals(normalized) || "Signed".equals(normalized);
    }

    public boolean isValidPaymentStatus(String value) {
        if (value == null) return false;
        String normalized = normalizePaymentStatus(value);
        return "Pending".equals(normalized) || "Received".equals(normalized) || "Overdue".equals(normalized);
    }

    public boolean isValidPaymentMethod(String value) {
        if (value == null) return false;
        String normalized = normalizePaymentMethod(value);
        return "Bank".equals(normalized) || "Transfer".equals(normalized) || "Cash".equals(normalized) || "Card".equals(normalized);
    }

    private String normalizeByMap(String value, Map<String, String> mapping) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return "";
        String key = toKey(trimmed);
        if (mapping.containsKey(key)) {
            return mapping.get(key);
        }
        return trimmed;
    }

    private String toKey(String value) {
        return value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }
}

