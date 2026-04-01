package com.yao.crm.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

import static com.yao.crm.service.ReportUtils.castMap;

/**
 * 报表导出服务 - 处理 CSV 导出相关功能
 */
@Service
public class ReportExportService {

    private final ReportService reportService;

    public ReportExportService(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * 导出概览报表 CSV（默认租户）
     */
    public String exportOverviewCsv(LocalDate fromDate, LocalDate toDate, String role) {
        throw new IllegalStateException("tenant_id_required");
    }

    /**
     * 导出概览报表 CSV（指定租户，默认语言）
     */
    public String exportOverviewCsvByTenant(String tenantId, LocalDate fromDate, LocalDate toDate, String role, String owner, String department) {
        return exportOverviewCsvByTenant(tenantId, fromDate, toDate, role, owner, department, "en");
    }

    /**
     * 导出概览报表 CSV（完整参数）
     */
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

    /**
     * 将报表数据转换为 CSV 格式
     */
    private String toCsv(Map<String, Object> report, LocalDate fromDate, LocalDate toDate, String role, String language) {
        Map<String, Object> summary = castMap(report.get("summary"));
        Map<String, Integer> taskStatus = castMap(report.get("taskStatus"));
        boolean zh = language != null && language.trim().toLowerCase().startsWith("zh");

        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append(zh ? "分组,字段,值\n" : "section,key,value\n");
        sb.append(row(zh ? "筛选" : "filter", zh ? "开始" : "from", fromDate == null ? "" : fromDate.toString()));
        sb.append(row(zh ? "筛选" : "filter", zh ? "结束" : "to", toDate == null ? "" : toDate.toString()));
        sb.append(row(zh ? "筛选" : "filter", zh ? "角色" : "role", role == null ? "" : role.trim().toUpperCase()));

        appendMapRows(sb, zh ? "汇总" : "summary", summary);
        appendMapRows(sb, zh ? "客户负责人分布" : "customerByOwner", castMap(report.get("customerByOwner")));
        appendMapRows(sb, zh ? "客户状态营收" : "revenueByStatus", castMap(report.get("revenueByStatus")));
        appendMapRows(sb, zh ? "商机阶段分布" : "opportunityByStage", castMap(report.get("opportunityByStage")));
        appendMapRows(sb, zh ? "任务状态" : "taskStatus", taskStatus);
        appendMapRows(sb, zh ? "跟进渠道分布" : "followUpByChannel", castMap(report.get("followUpByChannel")));
        appendMapRows(sb, zh ? "报价状态分布" : "quoteByStatus", castMap(report.get("quoteByStatus")));
        appendMapRows(sb, zh ? "订单状态分布" : "orderByStatus", castMap(report.get("orderByStatus")));
        return sb.toString();
    }

    /**
     * 追加 Map 数据行到 StringBuilder
     */
    private void appendMapRows(StringBuilder sb, String section, Map<String, ?> map) {
        if (map == null) {
            return;
        }
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            sb.append(row(section, entry.getKey(), entry.getValue()));
        }
    }

    /**
     * 构建单行 CSV 数据
     */
    private String row(String section, String key, Object value) {
        return csv(section) + "," + csv(key) + "," + csv(value == null ? "" : String.valueOf(value)) + "\n";
    }

    /**
     * CSV 字段转义处理
     */
    private String csv(String text) {
        String safe = text == null ? "" : text;
        String escaped = safe.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
