package com.yao.crm.service;

import com.yao.crm.entity.FieldPermission;
import com.yao.crm.entity.SensitiveFieldConfig;
import com.yao.crm.repository.FieldPermissionRepository;
import com.yao.crm.repository.SensitiveFieldConfigRepository;
import com.yao.crm.enums.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 字段级权限服务
 * 支持字段级的查看、编辑权限控制和数据脱敏
 */
@Service
public class FieldPermissionService {

    private final FieldPermissionRepository fieldPermissionRepository;
    private final SensitiveFieldConfigRepository sensitiveFieldConfigRepository;

    // 默认敏感字段配置（国内特色）
    private static final Map<String, String> DEFAULT_SENSITIVE_FIELDS = new HashMap<>();

    static {
        // 手机号
        DEFAULT_SENSITIVE_FIELDS.put("phone", "PHONE");
        DEFAULT_SENSITIVE_FIELDS.put("mobile", "PHONE");
        DEFAULT_SENSITIVE_FIELDS.put("telephone", "PHONE");
        // 身份证
        DEFAULT_SENSITIVE_FIELDS.put("idCard", "ID_CARD");
        DEFAULT_SENSITIVE_FIELDS.put("id_card", "ID_CARD");
        DEFAULT_SENSITIVE_FIELDS.put("identity", "ID_CARD");
        // 银行卡
        DEFAULT_SENSITIVE_FIELDS.put("bankCard", "BANK_CARD");
        DEFAULT_SENSITIVE_FIELDS.put("bank_card", "BANK_CARD");
        // 邮箱（可选）
        DEFAULT_SENSITIVE_FIELDS.put("email", "EMAIL");
    }

    public FieldPermissionService(
            FieldPermissionRepository fieldPermissionRepository,
            SensitiveFieldConfigRepository sensitiveFieldConfigRepository) {
        this.fieldPermissionRepository = fieldPermissionRepository;
        this.sensitiveFieldConfigRepository = sensitiveFieldConfigRepository;
    }

    /**
     * 检查字段权限
     */
    @Transactional(readOnly = true)
    public FieldPermissionResult checkFieldPermission(String tenantId, String role, String entityType, String fieldName) {
        Optional<FieldPermission> permission = fieldPermissionRepository
                .findByTenantIdAndRoleAndEntityTypeAndFieldName(tenantId, role, entityType, fieldName);

        if (permission.isPresent()) {
            FieldPermission fp = permission.get();
            return new FieldPermissionResult(
                    fp.getCanView(),
                    fp.getCanEdit(),
                    fp.getCanDelete(),
                    fp.getIsHidden()
            );
        }

        // 默认权限：所有人都可以查看和编辑
        return new FieldPermissionResult(true, true, UserRole.isAdmin(role) || UserRole.isManager(role), false);
    }

    /**
     * 批量检查字段权限
     */
    @Transactional(readOnly = true)
    public Map<String, FieldPermissionResult> checkFieldPermissions(
            String tenantId, String role, String entityType, List<String> fieldNames) {

        Map<String, FieldPermissionResult> results = new HashMap<>();
        List<FieldPermission> permissions = fieldPermissionRepository
                .findByTenantIdAndRoleAndEntityType(tenantId, role, entityType);

        Map<String, FieldPermission> permMap = new HashMap<>();
        permissions.forEach(p -> permMap.put(p.getFieldName(), p));

        for (String fieldName : fieldNames) {
            FieldPermission fp = permMap.get(fieldName);
            if (fp != null) {
                results.put(fieldName, new FieldPermissionResult(
                        fp.getCanView(),
                        fp.getCanEdit(),
                        fp.getCanDelete(),
                        fp.getIsHidden()
                ));
            } else {
                // 默认权限
                results.put(fieldName, new FieldPermissionResult(true, true, UserRole.isAdmin(role), false));
            }
        }

        return results;
    }

    /**
     * 设置字段权限
     */
    @Transactional(timeout = 30)
    public FieldPermission setFieldPermission(
            String tenantId, String role, String entityType, String fieldName,
            boolean canView, boolean canEdit, boolean canDelete, boolean isHidden) {

        Optional<FieldPermission> existing = fieldPermissionRepository
                .findByTenantIdAndRoleAndEntityTypeAndFieldName(tenantId, role, entityType, fieldName);

        FieldPermission permission;
        if (existing.isPresent()) {
            permission = existing.get();
        } else {
            permission = new FieldPermission();
            permission.setId(UUID.randomUUID().toString());
            permission.setTenantId(tenantId);
            permission.setRole(role);
            permission.setEntityType(entityType);
            permission.setFieldName(fieldName);
        }

        permission.setCanView(canView);
        permission.setCanEdit(canEdit);
        permission.setCanDelete(canDelete);
        permission.setIsHidden(isHidden);

        return fieldPermissionRepository.save(permission);
    }

    /**
     * 获取敏感字段配置
     */
    @Transactional(readOnly = true)
    public Optional<SensitiveFieldConfig> getSensitiveFieldConfig(String tenantId, String entityType, String fieldName) {
        return sensitiveFieldConfigRepository
                .findByTenantIdAndEntityTypeAndFieldName(tenantId, entityType, fieldName);
    }

    /**
     * 获取实体的所有敏感字段配置
     */
    @Transactional(readOnly = true)
    public List<SensitiveFieldConfig> getSensitiveFields(String tenantId, String entityType) {
        return sensitiveFieldConfigRepository.findByTenantIdAndEntityType(tenantId, entityType);
    }

    /**
     * 配置敏感字段
     */
    @Transactional(timeout = 30)
    public SensitiveFieldConfig configureSensitiveField(
            String tenantId, String entityType, String fieldName,
            String maskType, String maskPattern, int visibleStart, int visibleEnd, String maskChar) {

        Optional<SensitiveFieldConfig> existing = sensitiveFieldConfigRepository
                .findByTenantIdAndEntityTypeAndFieldName(tenantId, entityType, fieldName);

        SensitiveFieldConfig config;
        if (existing.isPresent()) {
            config = existing.get();
        } else {
            config = new SensitiveFieldConfig();
            config.setId(UUID.randomUUID().toString());
            config.setTenantId(tenantId);
            config.setEntityType(entityType);
            config.setFieldName(fieldName);
        }

        config.setMaskType(maskType);
        config.setMaskPattern(maskPattern);
        config.setVisibleCharsStart(visibleStart);
        config.setVisibleCharsEnd(visibleEnd);
        config.setMaskChar(maskChar);

        return sensitiveFieldConfigRepository.save(config);
    }

    /**
     * 脱敏处理
     */
    @Transactional(readOnly = true)
    public String maskSensitiveData(String tenantId, String entityType, String fieldName, String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        // 检查是否有自定义配置
        Optional<SensitiveFieldConfig> config = getSensitiveFieldConfig(tenantId, entityType, fieldName);
        if (config.isPresent()) {
            return config.get().mask(value);
        }

        // 检查是否是默认敏感字段
        String sensitiveType = DEFAULT_SENSITIVE_FIELDS.get(fieldName.toLowerCase());
        if (sensitiveType != null) {
            return applyDefaultMask(value, sensitiveType);
        }

        return value;
    }

    /**
     * 批量脱敏处理
     */
    @Transactional(readOnly = true)
    public Map<String, Object> maskSensitiveData(String tenantId, String entityType, Map<String, Object> data) {
        Map<String, Object> maskedData = new HashMap<>(data);

        for (String fieldName : data.keySet()) {
            String sensitiveType = DEFAULT_SENSITIVE_FIELDS.get(fieldName.toLowerCase());
            if (sensitiveType != null) {
                Object value = data.get(fieldName);
                if (value != null) {
                    maskedData.put(fieldName, applyDefaultMask(value.toString(), sensitiveType));
                }
            }
        }

        return maskedData;
    }

    /**
     * 应用默认脱敏规则
     */
    private String applyDefaultMask(String value, String type) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        switch (type) {
            case "PHONE":
                // 手机号：138****5678
                if (value.length() >= 11) {
                    return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
                }
                return "***";

            case "ID_CARD":
                // 身份证：430***********1234
                if (value.length() >= 18) {
                    return value.substring(0, 3) + "***********" + value.substring(value.length() - 4);
                }
                return "*****************";

            case "BANK_CARD":
                // 银行卡：6222 **** **** 1234
                if (value.length() >= 16) {
                    return value.substring(0, 4) + " **** **** " + value.substring(value.length() - 4);
                }
                return "**** **** **** ****";

            case "EMAIL":
                // 邮箱：t***@example.com
                int atIndex = value.indexOf('@');
                if (atIndex > 1) {
                    return value.substring(0, 1) + "***" + value.substring(atIndex);
                }
                return "***";

            default:
                return value;
        }
    }

    /**
     * 获取字段权限矩阵
     */
    @Transactional(readOnly = true)
    public Map<String, Map<String, FieldPermissionResult>> getPermissionMatrix(String tenantId) {
        List<FieldPermission> permissions = fieldPermissionRepository.findByTenantId(tenantId);

        Map<String, Map<String, FieldPermissionResult>> matrix = new HashMap<>();

        for (FieldPermission p : permissions) {
            String key = p.getEntityType() + "." + p.getFieldName();
            Map<String, FieldPermissionResult> roleMap = matrix.computeIfAbsent(key, k -> new HashMap<>());
            roleMap.put(p.getRole(), new FieldPermissionResult(
                    p.getCanView(),
                    p.getCanEdit(),
                    p.getCanDelete(),
                    p.getIsHidden()
            ));
        }

        return matrix;
    }

    /**
     * 字段权限结果
     */
    public static class FieldPermissionResult {
        private final boolean canView;
        private final boolean canEdit;
        private final boolean canDelete;
        private final boolean isHidden;

        public FieldPermissionResult(boolean canView, boolean canEdit, boolean canDelete, boolean isHidden) {
            this.canView = canView;
            this.canEdit = canEdit;
            this.canDelete = canDelete;
            this.isHidden = isHidden;
        }

        public boolean canView() { return canView; }
        public boolean canEdit() { return canEdit; }
        public boolean canDelete() { return canDelete; }
        public boolean isHidden() { return isHidden; }
    }
}
