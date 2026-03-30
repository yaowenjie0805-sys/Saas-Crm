package com.yao.crm.controller;

import com.yao.crm.entity.Customer;
import com.yao.crm.entity.Opportunity;
import com.yao.crm.entity.OrderRecord;
import com.yao.crm.service.I18nService;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 商业控制器共享辅助方法
 */
class CommerceControllerSupport extends BaseApiController {

    CommerceControllerSupport(I18nService i18nService) {
        super(i18nService);
    }

    /**
     * 生成新ID - 使用ThreadLocalRandom避免并发碰撞
     */
    protected String newId(String prefix) {
        return prefix + "_" + Long.toString(System.currentTimeMillis(), 36)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    /**
     * 生成编号
     */
    protected String generateNo(String prefix) {
        return prefix + "-" + LocalDate.now().toString().replace("-", "")
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    /**
     * 检查客户是否属于租户
     */
    protected boolean belongsToTenant(Customer customer, String tenantId) {
        return customer != null && tenantId.equals(customer.getTenantId());
    }

    /**
     * 检查商机是否属于租户
     */
    protected boolean belongsToTenant(Opportunity row, String tenantId) {
        return row != null && tenantId.equals(row.getTenantId());
    }

    /**
     * 转换为大写或返回默认值
     */
    protected String upperOrDefault(String value, String fallback) {
        if (isBlank(value)) return fallback;
        return value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 读取长整型值
     */
    protected long readLong(Object value, long fallback) {
        try { return Long.parseLong(String.valueOf(value)); } catch (Exception ex) { return fallback; }
    }

    /**
     * 读取整型值
     */
    protected int readInt(Object value, int fallback) {
        try { return Integer.parseInt(String.valueOf(value)); } catch (Exception ex) { return fallback; }
    }

    /**
     * 读取双精度值
     */
    protected double readDouble(Object value, double fallback) {
        try { return Double.parseDouble(String.valueOf(value)); } catch (Exception ex) { return fallback; }
    }

    /**
     * 清理文本
     */
    protected String sanitizeText(Object value, int maxLength, boolean trim) {
        if (value == null) return "";
        String text = String.valueOf(value);
        text = text.replaceAll("[\\u0000-\\u001F\\u007F]", "");
        if (trim) {
            text = text.trim();
        }
        if (text.length() > Math.max(1, maxLength)) {
            return text.substring(0, Math.max(1, maxLength));
        }
        return text;
    }

    /**
     * 转换为字符串
     */
    protected String str(Object value) {
        return sanitizeText(value, 512, false);
    }

    /**
     * 转换为可空字符串
     */
    protected String nullable(Object value) {
        String text = sanitizeText(value, 1024, true);
        return text.isEmpty() ? null : text;
    }

    /**
     * 分页响应体
     */
    protected Map<String, Object> pageBody(Page<?> pageResult, int page, int size) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", pageResult.getContent());
        body.put("total", pageResult.getTotalElements());
        body.put("page", Math.max(1, page));
        body.put("size", Math.max(1, size));
        body.put("totalPages", pageResult.getTotalPages());
        return body;
    }

    /**
     * 判断字符串是否为空
     */
    @Override
    protected boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * 转换订单记录为视图对象
     */
    protected Map<String, Object> toOrderView(OrderRecord row) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", row.getId());
        out.put("orderNo", row.getOrderNo());
        out.put("customerId", row.getCustomerId());
        out.put("opportunityId", row.getOpportunityId());
        out.put("quoteId", row.getQuoteId());
        out.put("owner", row.getOwner());
        out.put("status", row.getStatus());
        out.put("amount", row.getAmount());
        out.put("signDate", row.getSignDate());
        out.put("notes", row.getNotes());
        out.put("tenantId", row.getTenantId());
        out.put("updatedAt", row.getUpdatedAt());
        // 扩展字段
        out.put("settlementCurrency", row.getSettlementCurrency());
        out.put("exchangeRateSnapshot", row.getExchangeRateSnapshot());
        out.put("invoiceStatus", row.getInvoiceStatus());
        out.put("taxDisplayMode", row.getTaxDisplayMode());
        out.put("complianceTag", row.getComplianceTag());
        return out;
    }
}
