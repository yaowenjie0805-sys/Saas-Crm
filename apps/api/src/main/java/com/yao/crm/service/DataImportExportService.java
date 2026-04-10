package com.yao.crm.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 通用数据导入导出服务（门面类）
 * 支持多种实体的批量导入导出，包括客户、联系人、线索、产品等
 * 
 * 注意：此类现在作为门面类，所有实际逻辑已委托给以下服务：
 * - FileParsingService: 文件解析逻辑
 * - DataMappingService: 数据映射/转换逻辑
 * - DataImportService: 导入业务逻辑
 * - DataExportService: 导出业务逻辑
 */
@Service
@Slf4j
public class DataImportExportService {

    private final DataImportService dataImportService;
    private final DataExportService dataExportService;
    private final DataMappingService dataMappingService;

    public DataImportExportService(
            DataImportService dataImportService,
            DataExportService dataExportService,
            DataMappingService dataMappingService) {
        this.dataImportService = dataImportService;
        this.dataExportService = dataExportService;
        this.dataMappingService = dataMappingService;
    }

    // ========== 实体类型枚举（委托给 DataMappingService） ==========

    public enum EntityType {
        CUSTOMER("Customer", "客户"),
        CONTACT("Contact", "联系人"),
        LEAD("Lead", "线索"),
        PRODUCT("Product", "产品"),
        OPPORTUNITY("Opportunity", "商机"),
        CONTRACT("Contract", "合同");

        private final String code;
        private final String name;

        EntityType(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getCode() { return code; }
        public String getName() { return name; }

        public static EntityType fromCode(String code) {
            return EntityType.valueOf(DataMappingService.EntityType.fromCode(code).name());
        }
    }

    // ========== 导入相关方法（委托给 DataImportService） ==========

    /**
     * 创建导入任务
     */
    public ImportJobResult createImportJob(String tenantId, String operator, String entityType,
                                           InputStream inputStream, String fileName, String fileExtension) {
        DataImportService.ImportJobResult result = dataImportService.createImportJob(
                tenantId, operator, entityType, inputStream, fileName, fileExtension);
        return convertImportJobResult(result);
    }

    /**
     * 获取导入任务状态
     */
    public ImportJobResult getImportJobStatus(String jobId) {
        DataImportService.ImportJobResult result = dataImportService.getImportJobStatus(jobId);
        return convertImportJobResult(result);
    }

    /**
     * 转换导入结果类型
     */
    private ImportJobResult convertImportJobResult(DataImportService.ImportJobResult result) {
        if (result == null) {
            return null;
        }
        return new ImportJobResult(
                result.getJobId(),
                result.getStatus(),
                result.getTotalRows(),
                result.getProcessedRows(),
                result.getSuccessCount(),
                result.getFailCount(),
                result.getErrorMessage(),
                convertImportErrors(result.getErrors())
        );
    }

    /**
     * 转换导入错误列表
     */
    private List<ImportError> convertImportErrors(List<DataImportService.ImportError> errors) {
        if (errors == null) {
            return null;
        }
        List<ImportError> result = new java.util.ArrayList<>();
        for (DataImportService.ImportError error : errors) {
            ImportError e = new ImportError();
            e.setRowNumber(error.getRowNumber());
            e.setErrorMessage(error.getErrorMessage());
            e.setRawData(error.getRawData());
            result.add(e);
        }
        return result;
    }

    /**
     * 取消导入任务
     */
    public void cancelImportJob(String jobId) {
        dataImportService.cancelImportJob(jobId);
    }

    // ========== 导出相关方法（委托给 DataExportService） ==========

    /**
     * 创建导出任务
     */
    public ExportJobResult createExportJob(String tenantId, String operator, String entityType,
                                          Map<String, Object> filters, List<String> fields,
                                          String format) {
        DataExportService.ExportJobResult result = dataExportService.createExportJob(
                tenantId, operator, entityType, filters, fields, format);
        return convertExportJobResult(result);
    }

    /**
     * 获取导出任务状态
     */
    public ExportJobResult getExportJobStatus(String tenantId, String operator, String jobId, boolean canViewAll) {
        DataExportService.ExportJobResult result = dataExportService.getExportJobStatus(
                tenantId, operator, jobId, canViewAll
        );
        return convertExportJobResult(result);
    }

    /**
     * 转换导出结果类型
     */
    private ExportJobResult convertExportJobResult(DataExportService.ExportJobResult result) {
        if (result == null) {
            return null;
        }
        return new ExportJobResult(
                result.getJobId(),
                result.getStatus(),
                result.getTotalRows(),
                result.getFilePath(),
                result.getErrorMessage()
        );
    }

    /**
     * 获取导出文件
     */
    public byte[] getExportFile(String tenantId, String operator, String jobId, boolean canViewAll) throws IOException {
        return dataExportService.getExportFile(tenantId, operator, jobId, canViewAll);
    }

    /**
     * 获取导入模板
     */
    public byte[] getImportTemplate(String entityType, String format) throws IOException {
        return dataExportService.getImportTemplate(entityType, format);
    }

    // ========== 结果类（委托给子服务的结果类） ==========

    public static class ImportError {
        private int rowNumber;
        private String errorMessage;
        private String rawData;

        public int getRowNumber() { return rowNumber; }
        public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public String getRawData() { return rawData; }
        public void setRawData(String rawData) { this.rawData = rawData; }
    }

    public static class ImportJobResult {
        private final String jobId;
        private final String status;
        private final int totalRows;
        private final int processedRows;
        private final int successCount;
        private final int failCount;
        private final String errorMessage;
        private final List<ImportError> errors;

        public ImportJobResult(String jobId, String status, int totalRows, int processedRows,
                               int successCount, int failCount) {
            this.jobId = jobId;
            this.status = status;
            this.totalRows = totalRows;
            this.processedRows = processedRows;
            this.successCount = successCount;
            this.failCount = failCount;
            this.errorMessage = null;
            this.errors = null;
        }

        public ImportJobResult(String jobId, String status, int totalRows, int processedRows,
                               int successCount, int failCount, String errorMessage) {
            this.jobId = jobId;
            this.status = status;
            this.totalRows = totalRows;
            this.processedRows = processedRows;
            this.successCount = successCount;
            this.failCount = failCount;
            this.errorMessage = errorMessage;
            this.errors = null;
        }

        public ImportJobResult(String jobId, String status, int totalRows, int processedRows,
                               int successCount, int failCount, String errorMessage,
                               List<ImportError> errors) {
            this.jobId = jobId;
            this.status = status;
            this.totalRows = totalRows;
            this.processedRows = processedRows;
            this.successCount = successCount;
            this.failCount = failCount;
            this.errorMessage = errorMessage;
            this.errors = errors;
        }

        // Getters
        public String getJobId() { return jobId; }
        public String getStatus() { return status; }
        public int getTotalRows() { return totalRows; }
        public int getProcessedRows() { return processedRows; }
        public int getSuccessCount() { return successCount; }
        public int getFailCount() { return failCount; }
        public String getErrorMessage() { return errorMessage; }
        public List<ImportError> getErrors() { return errors; }

        public int getProgressPercent() {
            return totalRows > 0 ? (processedRows * 100 / totalRows) : 0;
        }
    }

    public static class ExportJobResult {
        private final String jobId;
        private final String status;
        private final int totalRows;
        private final String filePath;
        private final String errorMessage;

        public ExportJobResult(String jobId, String status, int totalRows) {
            this.jobId = jobId;
            this.status = status;
            this.totalRows = totalRows;
            this.filePath = null;
            this.errorMessage = null;
        }

        public ExportJobResult(String jobId, String status, int totalRows, String errorMessage) {
            this.jobId = jobId;
            this.status = status;
            this.totalRows = totalRows;
            this.errorMessage = errorMessage;
            this.filePath = null;
        }

        public ExportJobResult(String jobId, String status, int totalRows, String filePath, String errorMessage) {
            this.jobId = jobId;
            this.status = status;
            this.totalRows = totalRows;
            this.filePath = filePath;
            this.errorMessage = errorMessage;
        }

        // Getters
        public String getJobId() { return jobId; }
        public String getStatus() { return status; }
        public int getTotalRows() { return totalRows; }
        public String getFilePath() { return filePath; }
        public String getErrorMessage() { return errorMessage; }
    }
}
