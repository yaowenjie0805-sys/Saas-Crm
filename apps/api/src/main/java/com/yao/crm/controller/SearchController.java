package com.yao.crm.controller;

import com.yao.crm.entity.SavedSearch;
import com.yao.crm.repository.SavedSearchRepository;
import com.yao.crm.service.GlobalSearchService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 全局搜索控制器
 * 提供搜索API、搜索历史、搜索建议等功能
 */
@RestController
@RequestMapping("/api/v2/search")
public class SearchController {

    private final GlobalSearchService globalSearchService;
    private final SavedSearchRepository savedSearchRepository;

    public SearchController(GlobalSearchService globalSearchService, SavedSearchRepository savedSearchRepository) {
        this.globalSearchService = globalSearchService;
        this.savedSearchRepository = savedSearchRepository;
    }

    /**
     * 全局搜索
     */
    @GetMapping
    public ResponseEntity<?> search(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String type) {

        if (q == null || q.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "search_query_required");
            return ResponseEntity.badRequest().body(error);
        }

        GlobalSearchService.SearchResult result = globalSearchService.search(tenantId, q.trim(), limit);

        // 如果指定了类型，只返回该类型的结果
        Map<String, Object> response = new HashMap<>();
        if (type != null && !type.isEmpty()) {
            List<GlobalSearchService.SearchResult.Item> filtered = result.getResults().getOrDefault(type, Collections.emptyList());
            response.put("results", filtered);
            response.put("total", filtered.size());
        } else {
            response.put("results", result.getResults());
            response.put("total", result.getTotal());
        }

        response.put("query", q);
        response.put("limit", limit);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取搜索建议（自动补全）
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> suggestions(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit) {

        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // TODO: 实现搜索建议逻辑
        // 目前返回空列表，后续可以基于历史搜索提供建议
        return ResponseEntity.ok(Collections.emptyList());
    }

    /**
     * 获取搜索历史
     */
    @GetMapping("/history")
    public ResponseEntity<List<SavedSearch>> getSearchHistory(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam String owner,
            @RequestParam(defaultValue = "10") int limit) {

        List<SavedSearch> history = savedSearchRepository.findByTenantIdAndOwnerOrderByLastUsedAtDesc(
                tenantId, owner, PageRequest.of(0, limit));

        return ResponseEntity.ok(history);
    }

    /**
     * 保存搜索
     */
    @PostMapping("/saved")
    public ResponseEntity<SavedSearch> saveSearch(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestBody SaveSearchRequest request) {

        SavedSearch saved = new SavedSearch();
        saved.setId(UUID.randomUUID().toString());
        saved.setTenantId(tenantId);
        saved.setOwner(request.owner);
        saved.setName(request.name);
        saved.setSearchType(request.searchType != null ? request.searchType : "GLOBAL");
        saved.setQueryJson(request.queryJson);
        saved.setIsShared(request.isShared != null && request.isShared);
        saved.setShareWithRoles(request.shareWithRoles);
        saved.setUsageCount(0);
        saved.setCreatedAt(LocalDateTime.now());
        saved.setUpdatedAt(LocalDateTime.now());

        savedSearchRepository.save(saved);

        return ResponseEntity.ok(saved);
    }

    /**
     * 获取保存的搜索列表
     */
    @GetMapping("/saved")
    public ResponseEntity<List<SavedSearch>> getSavedSearches(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam String owner) {

        List<SavedSearch> saved = savedSearchRepository.findByTenantIdAndOwner(tenantId, owner);
        return ResponseEntity.ok(saved);
    }

    /**
     * 删除保存的搜索
     */
    @DeleteMapping("/saved/{id}")
    public ResponseEntity<?> deleteSavedSearch(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id) {

        savedSearchRepository.deleteById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return ResponseEntity.ok(result);
    }

    /**
     * 更新搜索使用次数
     */
    @PostMapping("/saved/{id}/use")
    public ResponseEntity<?> incrementUsage(
            @PathVariable String id) {

        Optional<SavedSearch> savedOpt = savedSearchRepository.findById(id);
        if (savedOpt.isPresent()) {
            SavedSearch saved = savedOpt.get();
            saved.setUsageCount(saved.getUsageCount() + 1);
            saved.setLastUsedAt(LocalDateTime.now());
            savedSearchRepository.save(saved);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return ResponseEntity.ok(result);
    }

    /**
     * 保存搜索请求
     */
    public static class SaveSearchRequest {
        public String owner;
        public String name;
        public String searchType;
        public String queryJson;
        public Boolean isShared;
        public String shareWithRoles;
    }
}
