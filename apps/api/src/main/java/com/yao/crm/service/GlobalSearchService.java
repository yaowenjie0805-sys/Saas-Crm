package com.yao.crm.service;

import com.yao.crm.entity.SearchIndex;
import com.yao.crm.repository.SearchIndexRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 全局搜索服务
 * 支持跨模块搜索、中文拼音搜索、模糊匹配
 */
@Service
public class GlobalSearchService {

    private final SearchIndexRepository searchIndexRepository;
    private final PinyinService pinyinService;

    public GlobalSearchService(SearchIndexRepository searchIndexRepository, PinyinService pinyinService) {
        this.searchIndexRepository = searchIndexRepository;
        this.pinyinService = pinyinService;
    }

    /**
     * 索引业务对象
     */
    @Transactional(timeout = 30)
    public void indexEntity(String tenantId, String entityType, String entityId, String... searchableFields) {
        StringBuilder searchContent = new StringBuilder();
        StringBuilder pinyinContent = new StringBuilder();

        for (String field : searchableFields) {
            if (field != null) {
                searchContent.append(field).append(" ");
                pinyinContent.append(pinyinService.toFullPinyin(field)).append(" ");
            }
        }

        Optional<SearchIndex> existing = searchIndexRepository.findByTenantIdAndEntityTypeAndEntityId(
                tenantId, entityType, entityId);

        SearchIndex index;
        if (existing.isPresent()) {
            index = existing.get();
        } else {
            index = new SearchIndex();
            index.setId(UUID.randomUUID().toString());
            index.setTenantId(tenantId);
            index.setEntityType(entityType);
            index.setEntityId(entityId);
        }

        index.setSearchContent(searchContent.toString().trim());
        index.setPinyinContent(pinyinContent.toString().trim());
        index.setUpdatedAt(LocalDateTime.now());

        searchIndexRepository.save(index);
    }

    /**
     * 搜索业务对象
     */
    @Transactional(readOnly = true)
    public SearchResult search(String tenantId, String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new SearchResult(Collections.emptyMap(), 0);
        }

        String trimmedKeyword = keyword.trim();
        List<SearchIndex> results;

        // 优先使用全文索引搜索
        try {
            results = searchIndexRepository.fullTextSearch(tenantId, trimmedKeyword, limit);
        } catch (Exception e) {
            // 如果全文索引失败，使用模糊搜索作为降级方案
            results = searchIndexRepository.fuzzySearch(tenantId, trimmedKeyword, limit);
        }

        // 按实体类型分组
        Map<String, List<SearchResult.Item>> grouped = new HashMap<>();
        for (SearchIndex idx : results) {
            grouped.computeIfAbsent(idx.getEntityType(), k -> new ArrayList<>())
                    .add(new SearchResult.Item(
                            idx.getEntityId(),
                            idx.getEntityType(),
                            extractSnippet(idx.getSearchContent(), trimmedKeyword)
                    ));
        }

        return new SearchResult(grouped, results.size());
    }

    /**
     * 删除索引
     */
    @Transactional(timeout = 30)
    public void deleteIndex(String tenantId, String entityType, String entityId) {
        searchIndexRepository.deleteByTenantAndEntity(tenantId, entityType, entityId);
    }

    /**
     * 提取搜索结果片段
     */
    private String extractSnippet(String content, String keyword) {
        if (content == null || keyword == null) {
            return "";
        }

        int index = content.toLowerCase().indexOf(keyword.toLowerCase());
        if (index == -1) {
            return content.length() > 100 ? content.substring(0, 100) + "..." : content;
        }

        int start = Math.max(0, index - 30);
        int end = Math.min(content.length(), index + keyword.length() + 30);

        String snippet = content.substring(start, end);
        if (start > 0) snippet = "..." + snippet;
        if (end < content.length()) snippet = snippet + "...";

        return snippet;
    }

    /**
     * 搜索结果
     */
    public static class SearchResult {
        private final Map<String, List<Item>> results;
        private final int total;

        public SearchResult(Map<String, List<Item>> results, int total) {
            this.results = results;
            this.total = total;
        }

        public Map<String, List<Item>> getResults() {
            return results;
        }

        public int getTotal() {
            return total;
        }

        public static class Item {
            private final String id;
            private final String type;
            private final String snippet;

            public Item(String id, String type, String snippet) {
                this.id = id;
                this.type = type;
                this.snippet = snippet;
            }

            public String getId() {
                return id;
            }

            public String getType() {
                return type;
            }

            public String getSnippet() {
                return snippet;
            }
        }
    }
}
