package com.yao.crm.repository;

import com.yao.crm.entity.SearchIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchIndexRepository extends JpaRepository<SearchIndex, String> {

    Optional<SearchIndex> findByTenantIdAndEntityTypeAndEntityId(
            String tenantId, String entityType, String entityId);

    List<SearchIndex> findByTenantIdAndEntityType(String tenantId, String entityType);

    @Modifying
    @Query("DELETE FROM SearchIndex s WHERE s.tenantId = :tenantId AND s.entityType = :entityType AND s.entityId = :entityId")
    void deleteByTenantAndEntity(
            @Param("tenantId") String tenantId,
            @Param("entityType") String entityType,
            @Param("entityId") String entityId);

    @Query(value = "SELECT * FROM search_index WHERE tenant_id = :tenantId AND MATCH(search_content, pinyin_content) AGAINST(:keyword IN BOOLEAN MODE) LIMIT :limit", nativeQuery = true)
    List<SearchIndex> fullTextSearch(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword,
            @Param("limit") int limit);

    @Query(value = "SELECT * FROM search_index WHERE tenant_id = :tenantId AND (search_content LIKE %:keyword% OR pinyin_content LIKE %:keyword%) LIMIT :limit", nativeQuery = true)
    List<SearchIndex> fuzzySearch(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword,
            @Param("limit") int limit);
}
