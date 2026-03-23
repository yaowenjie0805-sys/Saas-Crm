package com.yao.crm.repository;

import com.yao.crm.entity.ChartTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChartTemplateRepository extends JpaRepository<ChartTemplate, String> {

    List<ChartTemplate> findByTenantId(String tenantId);

    List<ChartTemplate> findByTenantIdAndChartType(String tenantId, String chartType);

    List<ChartTemplate> findByTenantIdAndDatasetType(String tenantId, String datasetType);

    List<ChartTemplate> findByTenantIdAndVisibility(String tenantId, String visibility);

    List<ChartTemplate> findByTenantIdAndOwner(String tenantId, String owner);

    List<ChartTemplate> findByTenantIdAndIsSystemTrue(String tenantId);

    List<ChartTemplate> findByTenantIdAndIsSystemFalse(String tenantId);
}
