package com.yao.crm.controller;

import com.yao.crm.entity.QuickFilter;
import com.yao.crm.repository.QuickFilterRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v2/filters")
public class QuickFilterController {

    private final QuickFilterRepository quickFilterRepository;

    public QuickFilterController(QuickFilterRepository quickFilterRepository) {
        this.quickFilterRepository = quickFilterRepository;
    }

    @GetMapping("/quick")
    public ResponseEntity<?> getQuickFilters(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam String entityType,
            @RequestParam(required = false) String owner) {

        String normalizedTenantId = normalizeRequired(tenantId);
        if (normalizedTenantId == null) {
            return badRequest("tenantId");
        }

        String normalizedEntityType = normalizeRequired(entityType);
        if (normalizedEntityType == null) {
            return badRequest("entityType");
        }

        String normalizedOwner = normalizeOptional(owner);
        List<QuickFilter> filters;

        if (normalizedOwner != null) {
            filters = quickFilterRepository.findByTenantIdAndOwnerAndEntityTypeOrderByDisplayOrderAsc(
                    normalizedTenantId, normalizedOwner, normalizedEntityType);

            List<QuickFilter> publicFilters = quickFilterRepository
                    .findByTenantIdAndOwnerAndEntityTypeOrderByDisplayOrderAsc(
                            normalizedTenantId, "", normalizedEntityType);

            Set<String> addedNames = new HashSet<>();
            filters.removeIf(f -> {
                boolean duplicate = addedNames.contains(f.getName());
                if (!duplicate) addedNames.add(f.getName());
                return duplicate;
            });
            filters.addAll(publicFilters);
        } else {
            filters = quickFilterRepository.findByTenantIdAndEntityTypeOrderByDisplayOrderAsc(
                    normalizedTenantId, normalizedEntityType);
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

    @PostMapping("/quick")
    public ResponseEntity<?> createQuickFilter(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestBody CreateQuickFilterRequest request) {

        String normalizedTenantId = normalizeRequired(tenantId);
        if (normalizedTenantId == null) {
            return badRequest("tenantId");
        }

        if (request == null) {
            return badRequest("name");
        }

        String normalizedName = normalizeRequired(request.name);
        if (normalizedName == null) {
            return badRequest("name");
        }

        String normalizedEntityType = normalizeRequired(request.entityType);
        if (normalizedEntityType == null) {
            return badRequest("entityType");
        }

        QuickFilter filter = new QuickFilter();
        filter.setId(UUID.randomUUID().toString());
        filter.setTenantId(normalizedTenantId);
        String normalizedOwner = normalizeOptional(request.owner);
        filter.setOwner(normalizedOwner != null ? normalizedOwner : "");
        filter.setName(normalizedName);
        filter.setIcon(request.icon);
        filter.setEntityType(normalizedEntityType);
        filter.setFilterConfig(request.filterConfig);
        filter.setDisplayOrder(request.displayOrder != null ? request.displayOrder : 0);
        filter.setIsDefault(request.isDefault != null && request.isDefault);
        filter.setCreatedAt(LocalDateTime.now());
        filter.setUpdatedAt(LocalDateTime.now());

        quickFilterRepository.save(filter);
        return ResponseEntity.ok(filter);
    }

    @PutMapping("/quick/{id}")
    public ResponseEntity<?> updateQuickFilter(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id,
            @RequestBody UpdateQuickFilterRequest request) {

        String normalizedTenantId = normalizeRequired(tenantId);
        if (normalizedTenantId == null) {
            return badRequest("tenantId");
        }

        String normalizedId = normalizeRequired(id);
        if (normalizedId == null) {
            return badRequest("id");
        }

        if (request == null) {
            return badRequest("name");
        }

        Optional<QuickFilter> filterOpt = quickFilterRepository.findByTenantIdAndId(normalizedTenantId, normalizedId);
        if (!filterOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        QuickFilter filter = filterOpt.get();
        if (request.name != null) {
            String normalizedName = normalizeRequired(request.name);
            if (normalizedName == null) {
                return badRequest("name");
            }
            filter.setName(normalizedName);
        }
        if (request.owner != null) {
            String normalizedOwner = normalizeOptional(request.owner);
            filter.setOwner(normalizedOwner != null ? normalizedOwner : "");
        }
        if (request.entityType != null) {
            String normalizedEntityType = normalizeRequired(request.entityType);
            if (normalizedEntityType == null) {
                return badRequest("entityType");
            }
            filter.setEntityType(normalizedEntityType);
        }
        if (request.icon != null) filter.setIcon(request.icon);
        if (request.filterConfig != null) filter.setFilterConfig(request.filterConfig);
        if (request.displayOrder != null) filter.setDisplayOrder(request.displayOrder);
        if (request.isDefault != null) filter.setIsDefault(request.isDefault);
        filter.setUpdatedAt(LocalDateTime.now());

        quickFilterRepository.save(filter);
        return ResponseEntity.ok(filter);
    }

    @DeleteMapping("/quick/{id}")
    public ResponseEntity<?> deleteQuickFilter(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id) {

        String normalizedTenantId = normalizeRequired(tenantId);
        if (normalizedTenantId == null) {
            return badRequest("tenantId");
        }

        String normalizedId = normalizeRequired(id);
        if (normalizedId == null) {
            return badRequest("id");
        }

        int deleted = quickFilterRepository.deleteByTenantIdAndId(normalizedTenantId, normalizedId);
        if (deleted == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> parseFilterConfig(String filterConfig) {
        if (filterConfig == null || filterConfig.trim().isEmpty()) {
            return new HashMap<>();
        }

        try {
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

    public static class CreateQuickFilterRequest {
        public String owner;
        public String name;
        public String icon;
        public String entityType;
        public String filterConfig;
        public Integer displayOrder;
        public Boolean isDefault;
    }

    public static class UpdateQuickFilterRequest {
        public String name;
        public String owner;
        public String entityType;
        public String icon;
        public String filterConfig;
        public Integer displayOrder;
        public Boolean isDefault;
    }

    private String normalizeRequired(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeOptional(String value) {
        return normalizeRequired(value);
    }

    private ResponseEntity<Map<String, Object>> badRequest(String field) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", "bad_request");
        response.put("message", "invalid request payload");
        Map<String, Object> details = new HashMap<>();
        details.put("field", field);
        response.put("details", details);
        return ResponseEntity.badRequest().body(response);
    }
}
