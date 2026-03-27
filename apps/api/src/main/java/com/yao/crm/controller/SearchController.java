package com.yao.crm.controller;

import com.yao.crm.entity.SavedSearch;
import com.yao.crm.repository.SavedSearchRepository;
import com.yao.crm.service.GlobalSearchService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v2/search")
public class SearchController {

    private static final int MAX_SEARCH_LIMIT = 100;
    private static final int MAX_SUGGESTION_LIMIT = 20;
    private static final int MAX_HISTORY_LIMIT = 100;

    private final GlobalSearchService globalSearchService;
    private final SavedSearchRepository savedSearchRepository;

    public SearchController(GlobalSearchService globalSearchService, SavedSearchRepository savedSearchRepository) {
        this.globalSearchService = globalSearchService;
        this.savedSearchRepository = savedSearchRepository;
    }

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

        int safeLimit = clampLimit(limit, 20, MAX_SEARCH_LIMIT);
        GlobalSearchService.SearchResult result = globalSearchService.search(tenantId, q.trim(), safeLimit);

        Map<String, Object> response = new HashMap<>();
        if (type != null && !type.isEmpty()) {
            List<GlobalSearchService.SearchResult.Item> filtered =
                    result.getResults().getOrDefault(type, Collections.emptyList());
            response.put("results", filtered);
            response.put("total", filtered.size());
        } else {
            response.put("results", result.getResults());
            response.put("total", result.getTotal());
        }

        response.put("query", q);
        response.put("limit", safeLimit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> suggestions(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit) {

        int safeLimit = clampLimit(limit, 5, MAX_SUGGESTION_LIMIT);
        if (q == null || q.trim().isEmpty() || safeLimit <= 0) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // TODO: implement suggestion logic; keep existing behavior for now.
        return ResponseEntity.ok(Collections.emptyList());
    }

    @GetMapping("/history")
    public ResponseEntity<?> getSearchHistory(
            HttpServletRequest httpRequest,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) String owner,
            @RequestParam(defaultValue = "10") int limit) {

        String currentUser = resolveCurrentUser(httpRequest, owner);
        if (currentUser == null) {
            return ResponseEntity.badRequest().build();
        }
        int safeLimit = clampLimit(limit, 10, MAX_HISTORY_LIMIT);
        List<SavedSearch> history = savedSearchRepository.findByTenantIdAndOwnerOrderByLastUsedAtDesc(
                tenantId, currentUser, PageRequest.of(0, safeLimit));
        return ResponseEntity.ok(history);
    }

    @PostMapping("/saved")
    public ResponseEntity<?> saveSearch(
            HttpServletRequest httpRequest,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestBody SaveSearchRequest request) {

        String currentUser = resolveCurrentUser(httpRequest, request.owner);
        if (currentUser == null) {
            return ResponseEntity.badRequest().build();
        }

        SavedSearch saved = new SavedSearch();
        saved.setId(UUID.randomUUID().toString());
        saved.setTenantId(tenantId);
        saved.setOwner(currentUser);
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

    @GetMapping("/saved")
    public ResponseEntity<?> getSavedSearches(
            HttpServletRequest httpRequest,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) String owner) {

        String currentUser = resolveCurrentUser(httpRequest, owner);
        if (currentUser == null) {
            return ResponseEntity.badRequest().build();
        }
        List<SavedSearch> saved = savedSearchRepository.findByTenantIdAndOwner(tenantId, currentUser);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/saved/{id}")
    public ResponseEntity<?> deleteSavedSearch(
            HttpServletRequest httpRequest,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) String owner,
            @PathVariable String id) {

        String currentUser = resolveCurrentUser(httpRequest, owner);
        if (currentUser == null) {
            return ResponseEntity.badRequest().build();
        }
        long deletedCount = savedSearchRepository.deleteByIdAndTenantIdAndOwner(id, tenantId, currentUser);
        if (deletedCount == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/saved/{id}/use")
    public ResponseEntity<?> incrementUsage(
            HttpServletRequest httpRequest,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) String owner,
            @PathVariable String id) {

        String currentUser = resolveCurrentUser(httpRequest, owner);
        if (currentUser == null) {
            return ResponseEntity.badRequest().build();
        }
        Optional<SavedSearch> savedOpt = savedSearchRepository.findByIdAndTenantIdAndOwner(id, tenantId, currentUser);
        if (savedOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        SavedSearch saved = savedOpt.get();
        saved.setUsageCount(saved.getUsageCount() + 1);
        saved.setLastUsedAt(LocalDateTime.now());
        savedSearchRepository.save(saved);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return ResponseEntity.ok(result);
    }

    public static class SaveSearchRequest {
        public String owner;
        public String name;
        public String searchType;
        public String queryJson;
        public Boolean isShared;
        public String shareWithRoles;
    }

    private static int clampLimit(int raw, int fallback, int max) {
        if (raw <= 0) return fallback;
        return Math.min(raw, max);
    }

    private String resolveCurrentUser(HttpServletRequest request, String legacyUserId) {
        Object authUsername = request == null ? null : request.getAttribute("authUsername");
        if (authUsername != null && !String.valueOf(authUsername).trim().isEmpty()) {
            return String.valueOf(authUsername).trim();
        }
        if (legacyUserId != null && !legacyUserId.trim().isEmpty()) {
            return legacyUserId.trim();
        }
        return null;
    }
}
