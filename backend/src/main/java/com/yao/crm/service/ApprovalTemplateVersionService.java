package com.yao.crm.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.entity.ApprovalTemplate;
import com.yao.crm.entity.ApprovalTemplateVersion;
import com.yao.crm.repository.ApprovalTemplateRepository;
import com.yao.crm.repository.ApprovalTemplateVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ApprovalTemplateVersionService {

    private final ApprovalTemplateRepository templateRepository;
    private final ApprovalTemplateVersionRepository versionRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public ApprovalTemplateVersionService(ApprovalTemplateRepository templateRepository,
                                          ApprovalTemplateVersionRepository versionRepository,
                                          AuditLogService auditLogService,
                                          ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.versionRepository = versionRepository;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ApprovalTemplate publish(String tenantId, String operator, String operatorRole, ApprovalTemplate template) {
        template.setEnabled(true);
        template.setStatus("PUBLISHED");
        template = templateRepository.save(template);
        saveVersionSnapshot(tenantId, operator, template);
        auditLogService.record(operator, operatorRole, "PUBLISH", "APPROVAL_TEMPLATE", template.getId(), "Published template", tenantId);
        return template;
    }

    @Transactional
    public ApprovalTemplate rollback(String tenantId, String operator, String operatorRole, ApprovalTemplate template, int version) {
        Optional<ApprovalTemplateVersion> versionOpt = versionRepository.findByTenantIdAndTemplateIdAndVersion(tenantId, template.getId(), version);
        if (!versionOpt.isPresent()) {
            throw new IllegalArgumentException("approval_template_not_found");
        }
        ApprovalTemplateVersion snap = versionOpt.get();
        int nextVersion = (template.getVersion() == null ? 1 : template.getVersion()) + 1;
        template.setName(snap.getName());
        template.setRole(snap.getRole());
        template.setDepartment(snap.getDepartment());
        template.setApproverRoles(snap.getApproverRoles());
        template.setFlowDefinition(snap.getFlowDefinition());
        template.setStatus("PUBLISHED");
        template.setEnabled(true);
        template.setVersion(nextVersion);
        template = templateRepository.save(template);
        saveVersionSnapshot(tenantId, operator, template);
        auditLogService.record(operator, operatorRole, "ROLLBACK", "APPROVAL_TEMPLATE", template.getId(), "Rollback to version " + version, tenantId);
        return template;
    }

    public List<Map<String, Object>> listVersions(String tenantId, String templateId) {
        List<ApprovalTemplateVersion> versions = versionRepository.findByTenantIdAndTemplateIdOrderByVersionDesc(tenantId, templateId);
        Map<String, Object> activeFlow = new LinkedHashMap<String, Object>();
        Optional<ApprovalTemplate> currentTemplate = templateRepository.findByIdAndTenantId(templateId, tenantId);
        if (currentTemplate.isPresent()) {
            activeFlow = parseFlow(currentTemplate.get().getFlowDefinition());
        }
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (ApprovalTemplateVersion v : versions) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("id", v.getId());
            row.put("templateId", v.getTemplateId());
            row.put("version", v.getVersion());
            row.put("name", v.getName());
            row.put("bizType", v.getBizType());
            row.put("status", v.getStatus());
            row.put("publishedBy", v.getPublishedBy());
            row.put("publishedAt", v.getPublishedAt());
            row.put("summary", buildSummary(v.getFlowDefinition(), v.getApproverRoles()));
            Map<String, Object> flow = parseFlow(v.getFlowDefinition());
            row.put("flowDefinition", flow);
            row.put("diffSummary", buildDiffSummary(activeFlow, flow));
            out.add(row);
        }
        return out;
    }

    private void saveVersionSnapshot(String tenantId, String operator, ApprovalTemplate template) {
        ApprovalTemplateVersion ver = new ApprovalTemplateVersion();
        ver.setId(newId("apv"));
        ver.setTenantId(tenantId);
        ver.setTemplateId(template.getId());
        ver.setVersion(template.getVersion());
        ver.setBizType(template.getBizType());
        ver.setName(template.getName());
        ver.setRole(template.getRole());
        ver.setDepartment(template.getDepartment());
        ver.setApproverRoles(template.getApproverRoles());
        ver.setFlowDefinition(template.getFlowDefinition());
        ver.setStatus("PUBLISHED");
        ver.setPublishedBy(operator);
        versionRepository.save(ver);
    }

    private Map<String, Object> parseFlow(String flowDefinition) {
        if (flowDefinition == null || flowDefinition.trim().isEmpty()) return new LinkedHashMap<String, Object>();
        try {
            return objectMapper.readValue(flowDefinition, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            return new LinkedHashMap<String, Object>();
        }
    }

    private Map<String, Object> buildSummary(String flowDefinition, String approverRoles) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        Map<String, Object> flow = parseFlow(flowDefinition);
        Object nodes = flow.get("nodes");
        int nodeCount = nodes instanceof List ? ((List<?>) nodes).size() : 0;
        summary.put("nodeCount", nodeCount);
        summary.put("approverRoles", approverRoles == null ? "" : approverRoles.toUpperCase(Locale.ROOT));
        return summary;
    }

    private Map<String, Object> buildDiffSummary(Map<String, Object> activeFlow, Map<String, Object> targetFlow) {
        List<Map<String, Object>> activeNodes = toNodeList(activeFlow);
        List<Map<String, Object>> targetNodes = toNodeList(targetFlow);
        Map<String, Map<String, Object>> activeByKey = toNodeMap(activeNodes);
        Map<String, Map<String, Object>> targetByKey = toNodeMap(targetNodes);
        Set<String> keys = new HashSet<String>();
        keys.addAll(activeByKey.keySet());
        keys.addAll(targetByKey.keySet());
        List<Map<String, Object>> nodeChanges = new ArrayList<Map<String, Object>>();
        for (String key : keys) {
            Map<String, Object> a = activeByKey.get(key);
            Map<String, Object> b = targetByKey.get(key);
            if (a == null && b != null) {
                nodeChanges.add(nodeChange(key, "ADDED", Collections.<String, Object>emptyMap(), b));
                continue;
            }
            if (a != null && b == null) {
                nodeChanges.add(nodeChange(key, "REMOVED", a, Collections.<String, Object>emptyMap()));
                continue;
            }
            Map<String, Object> delta = compareNodeFields(a, b);
            if (!delta.isEmpty()) {
                nodeChanges.add(nodeChange(key, "UPDATED", a, b, delta));
            }
        }
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("activeNodeCount", activeNodes.size());
        summary.put("versionNodeCount", targetNodes.size());
        summary.put("nodeDelta", targetNodes.size() - activeNodes.size());
        summary.put("changes", nodeChanges);
        return summary;
    }

    private List<Map<String, Object>> toNodeList(Map<String, Object> flow) {
        Object nodes = flow.get("nodes");
        if (!(nodes instanceof List)) return new ArrayList<Map<String, Object>>();
        List<?> list = (List<?>) nodes;
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Object item : list) {
            if (item instanceof Map) out.add((Map<String, Object>) item);
        }
        return out;
    }

    private Map<String, Map<String, Object>> toNodeMap(List<Map<String, Object>> nodes) {
        Map<String, Map<String, Object>> out = new LinkedHashMap<String, Map<String, Object>>();
        for (int i = 0; i < nodes.size(); i++) {
            Map<String, Object> n = nodes.get(i);
            Object id = n.get("id");
            Object seq = n.get("seq");
            String key = id == null || String.valueOf(id).trim().isEmpty()
                    ? ("seq:" + String.valueOf(seq == null ? (i + 1) : seq))
                    : ("id:" + String.valueOf(id));
            out.put(key, n);
        }
        return out;
    }

    private Map<String, Object> compareNodeFields(Map<String, Object> active, Map<String, Object> target) {
        Map<String, Object> delta = new LinkedHashMap<String, Object>();
        putDelta(delta, "approverRoles", normalizeCsv(active.get("approverRoles")), normalizeCsv(target.get("approverRoles")));
        putDelta(delta, "slaMinutes", toStr(active.get("slaMinutes")), toStr(target.get("slaMinutes")));
        putDelta(delta, "escalateToRoles", normalizeCsv(active.get("escalateToRoles")), normalizeCsv(target.get("escalateToRoles")));
        Map<String, Object> activeCond = active.get("conditions") instanceof Map ? (Map<String, Object>) active.get("conditions") : new LinkedHashMap<String, Object>();
        Map<String, Object> targetCond = target.get("conditions") instanceof Map ? (Map<String, Object>) target.get("conditions") : new LinkedHashMap<String, Object>();
        putDelta(delta, "condition.amountMin", toStr(activeCond.get("amountMin")), toStr(targetCond.get("amountMin")));
        putDelta(delta, "condition.amountMax", toStr(activeCond.get("amountMax")), toStr(targetCond.get("amountMax")));
        putDelta(delta, "condition.role", toStr(activeCond.get("role")), toStr(targetCond.get("role")));
        putDelta(delta, "condition.department", toStr(activeCond.get("department")), toStr(targetCond.get("department")));
        return delta;
    }

    private void putDelta(Map<String, Object> delta, String key, String from, String to) {
        String left = from == null ? "" : from;
        String right = to == null ? "" : to;
        if (left.equals(right)) return;
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("from", left);
        item.put("to", right);
        delta.put(key, item);
    }

    private String normalizeCsv(Object value) {
        String raw = toStr(value);
        if (raw == null || raw.trim().isEmpty()) return "";
        String[] arr = raw.split(",");
        List<String> out = new ArrayList<String>();
        for (String s : arr) {
            String t = s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
            if (!t.isEmpty()) out.add(t);
        }
        Collections.sort(out);
        return String.join(",", out);
    }

    private String toStr(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Map<String, Object> nodeChange(String key, String type, Map<String, Object> from, Map<String, Object> to) {
        return nodeChange(key, type, from, to, new LinkedHashMap<String, Object>());
    }

    private Map<String, Object> nodeChange(String key, String type, Map<String, Object> from, Map<String, Object> to, Map<String, Object> delta) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("nodeKey", key);
        row.put("type", type);
        row.put("delta", delta);
        row.put("from", from);
        row.put("to", to);
        return row;
    }

    private String newId(String prefix) {
        return prefix + "_" + Long.toString(System.currentTimeMillis(), 36) + String.format("%03d", (int) (Math.random() * 1000));
    }
}
