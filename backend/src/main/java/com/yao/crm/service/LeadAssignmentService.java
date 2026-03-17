package com.yao.crm.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.dto.request.V1LeadAssignmentRuleRequest;
import com.yao.crm.entity.Lead;
import com.yao.crm.entity.LeadAssignmentRule;
import com.yao.crm.repository.LeadAssignmentRuleRepository;
import com.yao.crm.repository.LeadRepository;
import com.yao.crm.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class LeadAssignmentService {

    private final LeadAssignmentRuleRepository ruleRepository;
    private final LeadRepository leadRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public LeadAssignmentService(LeadAssignmentRuleRepository ruleRepository,
                                 LeadRepository leadRepository,
                                 UserAccountRepository userAccountRepository,
                                 AuditLogService auditLogService,
                                 ObjectMapper objectMapper) {
        this.ruleRepository = ruleRepository;
        this.leadRepository = leadRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> listRules(String tenantId) {
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (LeadAssignmentRule row : ruleRepository.findByTenantIdOrderByUpdatedAtDesc(tenantId)) {
            items.add(toView(row));
        }
        return items;
    }

    @Transactional
    public LeadAssignmentRule createRule(String tenantId, String operator, V1LeadAssignmentRuleRequest payload) {
        LeadAssignmentRule rule = new LeadAssignmentRule();
        rule.setId(newId("lar"));
        rule.setTenantId(tenantId);
        rule.setName(payload.getName().trim());
        rule.setEnabled(Boolean.TRUE.equals(payload.getEnabled()));
        rule.setMembersJson(writeMembers(sanitizeMembers(payload.getMembers())));
        rule.setRrCursor(0);
        rule = ruleRepository.save(rule);
        auditLogService.record(operator, "SYSTEM", "CREATE", "LEAD_ASSIGNMENT_RULE", rule.getId(), "Created lead assignment rule", tenantId);
        return rule;
    }

    @Transactional
    public LeadAssignmentRule patchRule(String tenantId, String ruleId, String operator, V1LeadAssignmentRuleRequest payload) {
        Optional<LeadAssignmentRule> optional = ruleRepository.findByIdAndTenantId(ruleId, tenantId);
        if (!optional.isPresent()) return null;
        LeadAssignmentRule row = optional.get();
        row.setName(payload.getName().trim());
        row.setEnabled(Boolean.TRUE.equals(payload.getEnabled()));
        row.setMembersJson(writeMembers(sanitizeMembers(payload.getMembers())));
        row = ruleRepository.save(row);
        auditLogService.record(operator, "SYSTEM", "UPDATE", "LEAD_ASSIGNMENT_RULE", row.getId(), "Updated lead assignment rule", tenantId);
        return row;
    }

    @Transactional
    public String assignLeadOwner(String tenantId, String operator, Lead lead, String requestedOwner, boolean useRule) {
        String owner = normalize(requestedOwner);
        if (!owner.isEmpty()) {
            lead.setOwner(owner);
            leadRepository.save(lead);
            auditLogService.record(operator, "SYSTEM", "ASSIGN", "LEAD", lead.getId(), "Assigned lead to " + owner, tenantId);
            return owner;
        }
        if (!useRule) return normalize(lead.getOwner());
        String resolved = pickByRule(tenantId);
        if (!resolved.isEmpty()) {
            lead.setOwner(resolved);
            leadRepository.save(lead);
            auditLogService.record(operator, "SYSTEM", "ASSIGN", "LEAD", lead.getId(), "Assigned lead by rule to " + resolved, tenantId);
            return resolved;
        }
        return normalize(lead.getOwner());
    }

    @Transactional
    public String assignOwnerForTenant(String tenantId) {
        return pickByRule(tenantId);
    }

    private String pickByRule(String tenantId) {
        Optional<LeadAssignmentRule> optional = ruleRepository.findFirstByTenantIdAndEnabledOrderByUpdatedAtDesc(tenantId, true);
        if (!optional.isPresent()) return "";
        LeadAssignmentRule rule = optional.get();
        List<Member> expanded = expandMembers(readMembers(rule.getMembersJson()), tenantId);
        if (expanded.isEmpty()) return "";
        int cursor = rule.getRrCursor() == null ? 0 : Math.max(0, rule.getRrCursor());
        int idx = cursor % expanded.size();
        Member chosen = expanded.get(idx);
        rule.setRrCursor((idx + 1) % expanded.size());
        ruleRepository.save(rule);
        return chosen.username;
    }

    private List<Member> expandMembers(List<Member> members, String tenantId) {
        List<Member> out = new ArrayList<Member>();
        for (Member row : members) {
            if (row == null || !row.enabled || row.weight < 1 || normalize(row.username).isEmpty()) continue;
            if (!userAccountRepository.findByUsernameAndTenantIdAndEnabledTrue(row.username, tenantId).isPresent()) continue;
            int safeWeight = Math.min(20, row.weight);
            for (int i = 0; i < safeWeight; i++) {
                out.add(row);
            }
        }
        return out;
    }

    private List<Member> sanitizeMembers(List<V1LeadAssignmentRuleRequest.Member> input) {
        if (input == null) return Collections.emptyList();
        List<Member> out = new ArrayList<Member>();
        for (V1LeadAssignmentRuleRequest.Member row : input) {
            if (row == null) continue;
            Member member = new Member();
            member.username = normalize(row.getUsername());
            member.weight = row.getWeight() == null ? 1 : Math.max(1, Math.min(20, row.getWeight()));
            member.enabled = row.getEnabled() == null || row.getEnabled();
            if (!member.username.isEmpty()) out.add(member);
        }
        return out;
    }

    private List<Member> readMembers(String raw) {
        try {
            List<Member> rows = objectMapper.readValue(raw == null ? "[]" : raw, new TypeReference<List<Member>>() {});
            return rows == null ? Collections.<Member>emptyList() : rows;
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private String writeMembers(List<Member> rows) {
        try {
            return objectMapper.writeValueAsString(rows == null ? Collections.emptyList() : rows);
        } catch (Exception ex) {
            return "[]";
        }
    }

    public Map<String, Object> toView(LeadAssignmentRule row) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("id", row.getId());
        out.put("tenantId", row.getTenantId());
        out.put("name", row.getName());
        out.put("enabled", row.getEnabled());
        out.put("members", readMembers(row.getMembersJson()));
        out.put("rrCursor", row.getRrCursor());
        out.put("createdAt", row.getCreatedAt());
        out.put("updatedAt", row.getUpdatedAt());
        return out;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String newId(String prefix) {
        return prefix + "_" + Long.toString(System.currentTimeMillis(), 36) + String.format("%03d", (int) (Math.random() * 1000));
    }

    public static class Member {
        public String username;
        public int weight;
        public boolean enabled = true;
    }
}
