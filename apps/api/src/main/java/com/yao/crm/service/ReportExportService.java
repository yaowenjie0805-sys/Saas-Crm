package com.yao.crm.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

import static com.yao.crm.service.ReportUtils.castMap;

@Service
public class ReportExportService {

    private final ReportService reportService;

    public ReportExportService(ReportService reportService) {
        this.reportService = reportService;
    }

    public String exportOverviewCsvByTenant(String tenantId, LocalDate fromDate, LocalDate toDate, String role, String owner, String department) {
        return exportOverviewCsvByTenant(tenantId, fromDate, toDate, role, owner, department, "en");
    }

    public String exportOverviewCsvByTenant(String tenantId,
                                            LocalDate fromDate,
                                            LocalDate toDate,
                                            String role,
                                            String owner,
                                            String department,
                                            String language) {
        String requiredTenantId = requireTenantId(tenantId);
        Map<String, Object> report = reportService.overviewByTenant(requiredTenantId, fromDate, toDate, role, owner, department);
        return toCsv(report, fromDate, toDate, role, language);
    }

    private String requireTenantId(String tenantId) {
        String normalized = tenantId == null ? "" : tenantId.trim();
        if (normalized.isEmpty()) {
            throw new IllegalStateException("tenant_id_required");
        }
        return normalized;
    }

    private String toCsv(Map<String, Object> report, LocalDate fromDate, LocalDate toDate, String role, String language) {
        Map<String, Object> summary = castMap(report.get("summary"));
        Map<String, Integer> taskStatus = castMap(report.get("taskStatus"));
        boolean zh = language != null && language.trim().toLowerCase().startsWith("zh");

        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append(zh ? "\u5206\u7EC4,\u5B57\u6BB5,\u503C\n" : "section,key,value\n");
        sb.append(row(zh ? "\u7B5B\u9009" : "filter", zh ? "\u5F00\u59CB" : "from", fromDate == null ? "" : fromDate.toString()));
        sb.append(row(zh ? "\u7B5B\u9009" : "filter", zh ? "\u7ED3\u675F" : "to", toDate == null ? "" : toDate.toString()));
        sb.append(row(zh ? "\u7B5B\u9009" : "filter", zh ? "\u89D2\u8272" : "role", role == null ? "" : role.trim().toUpperCase()));

        appendMapRows(sb, zh ? "\u6C47\u603B" : "summary", summary);
        appendMapRows(sb, zh ? "\u5BA2\u6237\u8D1F\u8D23\u4EBA\u5206\u5E03" : "customerByOwner", castMap(report.get("customerByOwner")));
        appendMapRows(sb, zh ? "\u5BA2\u6237\u72B6\u6001\u8425\u6536" : "revenueByStatus", castMap(report.get("revenueByStatus")));
        appendMapRows(sb, zh ? "\u5546\u673A\u9636\u6BB5\u5206\u5E03" : "opportunityByStage", castMap(report.get("opportunityByStage")));
        appendMapRows(sb, zh ? "\u4EFB\u52A1\u72B6\u6001" : "taskStatus", taskStatus);
        appendMapRows(sb, zh ? "\u8DDF\u8FDB\u6E20\u9053\u5206\u5E03" : "followUpByChannel", castMap(report.get("followUpByChannel")));
        appendMapRows(sb, zh ? "\u62A5\u4EF7\u72B6\u6001\u5206\u5E03" : "quoteByStatus", castMap(report.get("quoteByStatus")));
        appendMapRows(sb, zh ? "\u8BA2\u5355\u72B6\u6001\u5206\u5E03" : "orderByStatus", castMap(report.get("orderByStatus")));
        return sb.toString();
    }

    private void appendMapRows(StringBuilder sb, String section, Map<String, ?> map) {
        if (map == null) {
            return;
        }
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            sb.append(row(section, entry.getKey(), entry.getValue()));
        }
    }

    private String row(String section, String key, Object value) {
        return csv(section) + "," + csv(key) + "," + csv(value == null ? "" : String.valueOf(value)) + "\n";
    }

    private String csv(String text) {
        String safe = text == null ? "" : text;
        String escaped = safe.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
