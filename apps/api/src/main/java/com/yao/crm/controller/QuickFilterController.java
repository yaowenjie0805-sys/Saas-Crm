package com.yao.crm.controller;

import com.yao.crm.entity.QuickFilter;
import com.yao.crm.repository.QuickFilterRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 快捷筛选控制器 - 国内特色
 * 提供快捷筛选的CRUD操作
 */
@RestController
@RequestMapping("/api/v2/filters")
public class QuickFilterController {

    private final QuickFilterRepository quickFilterRepository;

    public QuickFilterController(QuickFilterRepository quickFilterRepository) {
        this.quickFilterRepository = quickFilterRepository;
    }

    /**
     * 获取快捷筛选列表
     */
    @GetMapping("/quick")
    public ResponseEntity<?> getQuickFilters(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam String entityType,
            @RequestParam(required = false) String owner) {

        List<QuickFilter> filters;

        if (owner != null && !owner.isEmpty()) {
            // 返回用户个人和公共筛选
            filters = quickFilterRepository.findByTenantIdAndOwnerAndEntityTypeOrderByDisplayOrderAsc(
                    tenantId, owner, entityType);

            // 合并公共筛选（owner为null或特殊值）
            List<QuickFilter> publicFilters = quickFilterRepository
                    .findByTenantIdAndOwnerAndEntityTypeOrderByDisplayOrderAsc(
                            tenantId, "", entityType);

            // 去重合并
            Set<String> addedNames = new HashSet<>();
            filters.removeIf(f -> {
                boolean duplicate = addedNames.contains(f.getName());
                if (!duplicate) addedNames.add(f.getName());
                return duplicate;
            });
            filters.addAll(publicFilters);
        } else {
            filters = quickFilterRepository.findByTenantIdAndEntityTypeOrderByDisplayOrderAsc(
                    tenantId, entityType);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        int order = 0;
        for (QuickFilter filter : filters) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", filter.getId());
            item.put("name", filter.getName());
            item.put("icon", filter.getIcon());
            item.put("entityType", filter.getEntityType());
            item.put("filterConfig", parseFilterConfig(filter.getFilterConfig()));
            item.put("isDefault", filter.getIsDefault());
            item.put("displayOrder", order++);
            result.add(item);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("filters", result);
        return ResponseEntity.ok(response);
    }

    /**
     * 创建快捷筛选
     */
    @PostMapping("/quick")
    public ResponseEntity<QuickFilter> createQuickFilter(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestBody CreateQuickFilterRequest request) {

        QuickFilter filter = new QuickFilter();
        filter.setId(UUID.randomUUID().toString());
        filter.setTenantId(tenantId);
        filter.setOwner(request.owner != null ? request.owner : "");
        filter.setName(request.name);
        filter.setIcon(request.icon);
        filter.setEntityType(request.entityType);
        filter.setFilterConfig(request.filterConfig);
        filter.setDisplayOrder(request.displayOrder != null ? request.displayOrder : 0);
        filter.setIsDefault(request.isDefault != null && request.isDefault);
        filter.setCreatedAt(LocalDateTime.now());
        filter.setUpdatedAt(LocalDateTime.now());

        quickFilterRepository.save(filter);

        return ResponseEntity.ok(filter);
    }

    /**
     * 更新快捷筛选
     */
    @PutMapping("/quick/{id}")
    public ResponseEntity<?> updateQuickFilter(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id,
            @RequestBody UpdateQuickFilterRequest request) {

        Optional<QuickFilter> filterOpt = quickFilterRepository.findById(id);
        if (!filterOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        QuickFilter filter = filterOpt.get();

        if (request.name != null) filter.setName(request.name);
        if (request.icon != null) filter.setIcon(request.icon);
        if (request.filterConfig != null) filter.setFilterConfig(request.filterConfig);
        if (request.displayOrder != null) filter.setDisplayOrder(request.displayOrder);
        if (request.isDefault != null) filter.setIsDefault(request.isDefault);
        filter.setUpdatedAt(LocalDateTime.now());

        quickFilterRepository.save(filter);

        return ResponseEntity.ok(filter);
    }

    /**
     * 删除快捷筛选
     */
    @DeleteMapping("/quick/{id}")
    public ResponseEntity<?> deleteQuickFilter(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id) {

        Optional<QuickFilter> filterOpt = quickFilterRepository.findById(id);
        if (!filterOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        quickFilterRepository.deleteById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return ResponseEntity.ok(result);
    }

    /**
     * 解析筛选配置JSON
     */
    private Map<String, Object> parseFilterConfig(String filterConfig) {
        if (filterConfig == null || filterConfig.trim().isEmpty()) {
            return new HashMap<>();
        }

        try {
            // 简单的JSON解析，实际项目中建议使用Jackson
            Map<String, Object> config = new HashMap<>();
            String content = filterConfig.trim();
            if (content.startsWith("{") && content.endsWith("}")) {
                content = content.substring(1, content.length() - 1);
                String[] pairs = content.split(",");
                for (String pair : pairs) {
                    String[] keyValue = pair.split(":");
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim().replace("\"", "");
                        String value = keyValue[1].trim().replace("\"", "");
                        config.put(key, value);
                    }
                }
            }
            return config;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * 创建请求
     */
    public static class CreateQuickFilterRequest {
        public String owner;
        public String name;
        public String icon;
        public String entityType;
        public String filterConfig;
        public Integer displayOrder;
        public Boolean isDefault;
    }

    /**
     * 更新请求
     */
    public static class UpdateQuickFilterRequest {
        public String name;
        public String icon;
        public String filterConfig;
        public Integer displayOrder;
        public Boolean isDefault;
    }
}
