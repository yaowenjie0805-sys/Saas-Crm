package com.yao.crm.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class ValueNormalizerService {

    private final Map<String, String> customerStatusMap = mapOf(
            "ACTIVE", "Active",
            "PENDING", "Pending",
            "INACTIVE", "Inactive",
            "NEGOTIATING", "Active",
            "QUOTATION_PENDING", "Pending",
            "FIRST_CONTACT", "Pending",
            "PROPOSAL_REVIEW", "Active"
    );

    private final Map<String, String> opportunityStageMap = mapOf(
            "LEAD", "Lead",
            "QUALIFIED", "Qualified",
            "PROPOSAL", "Proposal",
            "NEGOTIATION", "Negotiation",
            "CLOSED_WON", "Closed Won",
            "CLOSED_LOST", "Closed Lost",
            "LEAD_COLLECTION", "Lead",
            "INITIAL_CONTACT", "Lead",
            "PROPOSAL_FOLLOW_UP", "Proposal",
            "BUSINESS_NEGOTIATION", "Negotiation"
    );

    private final Map<String, String> contractStatusMap = mapOf(
            "DRAFT", "Draft",
            "SIGNED", "Signed"
    );

    private final Map<String, String> paymentStatusMap = mapOf(
            "PENDING", "Pending",
            "RECEIVED", "Received",
            "OVERDUE", "Overdue"
    );

    private final Map<String, String> paymentMethodMap = mapOf(
            "BANK", "Bank",
            "BANK_TRANSFER", "Bank",
            "TRANSFER", "Transfer",
            "CASH", "Cash",
            "CARD", "Card"
    );

    private final Map<String, String> followUpChannelMap = mapOf(
            "PHONE", "Phone",
            "EMAIL", "Email",
            "WECHAT", "WeChat",
            "VISIT", "Visit",
            "MEETING", "Meeting"
    );

    public String normalizeContractStatus(String value) {
        return normalizeByMap(value, contractStatusMap);
    }

    public String normalizeCustomerStatus(String value) {
        return normalizeByMap(value, customerStatusMap);
    }

    public String normalizeOpportunityStage(String value) {
        return normalizeByMap(value, opportunityStageMap);
    }

    public String normalizePaymentStatus(String value) {
        return normalizeByMap(value, paymentStatusMap);
    }

    public String normalizePaymentMethod(String value) {
        return normalizeByMap(value, paymentMethodMap);
    }

    public String normalizeFollowUpChannel(String value) {
        return normalizeByMap(value, followUpChannelMap);
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

    private Map<String, String> mapOf(String... values) {
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            map.put(values[i], values[i + 1]);
        }
        return map;
    }
}

