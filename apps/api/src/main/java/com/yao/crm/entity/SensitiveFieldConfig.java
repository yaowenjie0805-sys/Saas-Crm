package com.yao.crm.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 敏感字段配置实体 - 国内特色
 * 支持手机号、身份证等敏感字段的脱敏显示
 */
@Entity
@Table(name = "sensitive_field_configs")
public class SensitiveFieldConfig {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 40)
    private String entityType;

    @Column(nullable = false, length = 80)
    private String fieldName;

    @Column(nullable = false, length = 20)
    private String maskType;

    @Column(length = 100)
    private String maskPattern;

    @Column(nullable = false)
    private Integer visibleCharsStart;

    @Column(nullable = false)
    private Integer visibleCharsEnd;

    @Column(nullable = false, length = 10)
    private String maskChar;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (id == null) id = java.util.UUID.randomUUID().toString();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (maskType == null || maskType.trim().isEmpty()) maskType = "PARTIAL";
        if (visibleCharsStart == null) visibleCharsStart = 3;
        if (visibleCharsEnd == null) visibleCharsEnd = 4;
        if (maskChar == null || maskChar.trim().isEmpty()) maskChar = "*";
        if (tenantId == null || tenantId.trim().isEmpty()) throw new IllegalStateException("tenant_id_required");
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 脱敏处理
     */
    public String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        switch (maskType) {
            case "FULL":
                return repeatChar(maskChar, value.length());
            case "CUSTOM":
                return applyCustomPattern(value);
            case "PARTIAL":
            default:
                return applyPartialMask(value);
        }
    }

    /**
     * 部分脱敏：显示前N位和后M位，中间用*替代
     */
    private String applyPartialMask(String value) {
        int len = value.length();
        int start = visibleCharsStart;
        int end = visibleCharsEnd;

        if (len <= start + end) {
            return repeatChar(maskChar, len);
        }

        StringBuilder result = new StringBuilder();
        result.append(value.substring(0, start));

        int maskLen = len - start - end;
        result.append(repeatChar(maskChar, Math.max(1, maskLen)));

        result.append(value.substring(len - end));
        return result.toString();
    }

    /**
     * 自定义模式脱敏
     */
    private String applyCustomPattern(String value) {
        if (maskPattern == null || maskPattern.isEmpty()) {
            return applyPartialMask(value);
        }

        // 支持正则表达式模式
        // 例如: (\d{3})\d+(\d{4}) -> $1****$2 用于手机号
        try {
            return value.replaceAll(maskPattern.split("->")[0].trim(),
                    maskPattern.contains("->") ? maskPattern.split("->")[1].trim() : repeatChar(maskChar, 4));
        } catch (Exception e) {
            return applyPartialMask(value);
        }
    }

    /**
     * 重复字符n次（Java 8 兼容）
     */
    private String repeatChar(String ch, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getMaskType() { return maskType; }
    public void setMaskType(String maskType) { this.maskType = maskType; }

    public String getMaskPattern() { return maskPattern; }
    public void setMaskPattern(String maskPattern) { this.maskPattern = maskPattern; }

    public Integer getVisibleCharsStart() { return visibleCharsStart; }
    public void setVisibleCharsStart(Integer visibleCharsStart) { this.visibleCharsStart = visibleCharsStart; }

    public Integer getVisibleCharsEnd() { return visibleCharsEnd; }
    public void setVisibleCharsEnd(Integer visibleCharsEnd) { this.visibleCharsEnd = visibleCharsEnd; }

    public String getMaskChar() { return maskChar; }
    public void setMaskChar(String maskChar) { this.maskChar = maskChar; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
