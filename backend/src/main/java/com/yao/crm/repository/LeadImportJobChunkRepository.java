package com.yao.crm.repository;

import com.yao.crm.entity.LeadImportJobChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeadImportJobChunkRepository extends JpaRepository<LeadImportJobChunk, String> {
    Optional<LeadImportJobChunk> findByTenantIdAndJobIdAndChunkNo(String tenantId, String jobId, Integer chunkNo);
    List<LeadImportJobChunk> findByTenantIdAndJobIdOrderByChunkNoAsc(String tenantId, String jobId);
    long countByTenantIdAndJobId(String tenantId, String jobId);
    long countByTenantIdAndJobIdAndStatus(String tenantId, String jobId, String status);
}
