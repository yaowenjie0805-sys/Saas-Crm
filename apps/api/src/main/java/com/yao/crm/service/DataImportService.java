package com.yao.crm.service;

import com.yao.crm.entity.Contact;
import com.yao.crm.entity.Customer;
import com.yao.crm.entity.Lead;
import com.yao.crm.entity.Product;
import com.yao.crm.repository.ContactRepository;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.LeadRepository;
import com.yao.crm.repository.ProductRepository;
import com.yao.crm.service.DataMappingService.EntityType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 数据导入服务
 * 负责导入业务逻辑、任务管理、异步处理
 */
@Service
@Slf4j
public class DataImportService {

    // 导入任务状态
    public enum ImportJobStatus {
        PENDING, RUNNING, COMPLETED, FAILED, PARTIAL_SUCCESS, CANCELLED
    }

    // 导入任务缓存
    private final Map<String, ImportJobContext> importJobs = new ConcurrentHashMap<>();
    private static final long JOB_RETENTION_HOURS = 24L;
    private static final long CLEANUP_INTERVAL_MS = TimeUnit.MINUTES.toMillis(10);
    private static final int CLEANUP_TRIGGER_SIZE = 128;
    private final AtomicLong lastCleanupAt = new AtomicLong(0L);

    private final FileParsingService fileParsingService;
    private final DataMappingService dataMappingService;
    private final CustomerRepository customerRepository;
    private final ContactRepository contactRepository;
    private final LeadRepository leadRepository;
    private final ProductRepository productRepository;

    public DataImportService(
            FileParsingService fileParsingService,
            DataMappingService dataMappingService,
            CustomerRepository customerRepository,
            ContactRepository contactRepository,
            LeadRepository leadRepository,
            ProductRepository productRepository) {
        this.fileParsingService = fileParsingService;
        this.dataMappingService = dataMappingService;
        this.customerRepository = customerRepository;
        this.contactRepository = contactRepository;
        this.leadRepository = leadRepository;
        this.productRepository = productRepository;
    }

    /**
     * 创建导入任务
     */
    @Transactional
    public ImportJobResult createImportJob(String tenantId, String operator, String entityType,
                                           InputStream inputStream, String fileName, String fileExtension) {
        cleanupExpiredJobs();
        String jobId = UUID.randomUUID().toString();

        try {
            // 解析文件
            FileParsingService.ParsedData parsedData = fileParsingService.parseFile(inputStream, fileExtension, entityType);

            // 创建任务上下文
            ImportJobContext context = new ImportJobContext();
            context.setJobId(jobId);
            context.setTenantId(tenantId);
            context.setEntityType(entityType);
            context.setOperator(operator);
            context.setFileName(fileName);
            context.setStatus(ImportJobStatus.PENDING);
            context.setTotalRows(parsedData.getRows().size());
            context.setProcessedRows(0);
            context.setSuccessCount(0);
            context.setFailCount(0);
            context.setHeaders(parsedData.getHeaders());
            context.setData(parsedData.getRows());
            context.setErrors(new ArrayList<>());
            context.setCreatedAt(LocalDateTime.now());

            importJobs.put(jobId, context);

            // 异步处理导入
            processImportAsync(jobId);

            return new ImportJobResult(jobId, context.getStatus().name(), context.getTotalRows(),
                    context.getProcessedRows(), context.getSuccessCount(), context.getFailCount());

        } catch (Exception e) {
            log.error("Failed to create import job", e);
            return new ImportJobResult(null, ImportJobStatus.FAILED.name(), 0, 0, 0, 0, e.getMessage());
        }
    }

    /**
     * 异步处理导入
     */
    private void processImportAsync(String jobId) {
        ImportJobContext context = importJobs.get(jobId);
        if (context == null) return;

        context.setStatus(ImportJobStatus.RUNNING);

        try {
            List<Map<String, String>> data = context.getData();
            List<String> headers = context.getHeaders();
            EntityType entityType = EntityType.fromCode(context.getEntityType());

            for (int i = 0; i < data.size(); i++) {
                if (context.getStatus() == ImportJobStatus.CANCELLED) {
                    break;
                }

                Map<String, String> row = data.get(i);
                try {
                    Object entity = dataMappingService.mapRowToEntity(row, headers, entityType, context.getTenantId());
                    saveEntity(entity, entityType);
                    context.setSuccessCount(context.getSuccessCount() + 1);
                } catch (Exception e) {
                    context.setFailCount(context.getFailCount() + 1);
                    ImportError error = new ImportError();
                    error.setRowNumber(i + 2); // Excel行号从1开始，标题行是1
                    error.setErrorMessage(e.getMessage());
                    error.setRawData(dataMappingService.rowToString(row));
                    context.getErrors().add(error);
                }

                context.setProcessedRows(context.getProcessedRows() + 1);
            }

            // 更新最终状态
            if (context.getStatus() != ImportJobStatus.CANCELLED) {
                if (context.getFailCount() == 0) {
                    context.setStatus(ImportJobStatus.COMPLETED);
                } else if (context.getSuccessCount() > 0) {
                    context.setStatus(ImportJobStatus.PARTIAL_SUCCESS);
                } else {
                    context.setStatus(ImportJobStatus.FAILED);
                }
            }
            if (isTerminal(context.getStatus())) {
                markFinished(context);
                releaseTransientJobData(context);
            }

        } catch (Exception e) {
            log.error("Import job failed: jobId={}", jobId, e);
            context.setStatus(ImportJobStatus.FAILED);
            context.setErrorMessage(e.getMessage());
            markFinished(context);
            releaseTransientJobData(context);
        }
    }

    /**
     * 保存实体
     */
    private void saveEntity(Object entity, EntityType entityType) {
        switch (entityType) {
            case CUSTOMER:
                customerRepository.save((Customer) entity);
                break;
            case CONTACT:
                contactRepository.save((Contact) entity);
                break;
            case LEAD:
                leadRepository.save((Lead) entity);
                break;
            case PRODUCT:
                productRepository.save((Product) entity);
                break;
            default:
                throw new IllegalArgumentException("Cannot save entity type: " + entityType);
        }
    }

    /**
     * 获取导入任务状态
     */
    public ImportJobResult getImportJobStatus(String jobId) {
        cleanupExpiredJobs();
        ImportJobContext context = importJobs.get(jobId);
        if (context == null) {
            return null;
        }
        return new ImportJobResult(
                jobId,
                context.getStatus().name(),
                context.getTotalRows(),
                context.getProcessedRows(),
                context.getSuccessCount(),
                context.getFailCount(),
                context.getErrorMessage(),
                context.getErrors()
        );
    }

    /**
     * 取消导入任务
     */
    public void cancelImportJob(String jobId) {
        cleanupExpiredJobs();
        ImportJobContext context = importJobs.get(jobId);
        if (context != null && (context.getStatus() == ImportJobStatus.PENDING ||
                context.getStatus() == ImportJobStatus.RUNNING)) {
            context.setStatus(ImportJobStatus.CANCELLED);
            markFinished(context);
        }
    }

    private void cleanupExpiredJobs() {
        long now = System.currentTimeMillis();
        long previous = lastCleanupAt.get();
        if (importJobs.size() < CLEANUP_TRIGGER_SIZE && now - previous < CLEANUP_INTERVAL_MS) {
            return;
        }
        if (!lastCleanupAt.compareAndSet(previous, now)) {
            return;
        }
        LocalDateTime threshold = LocalDateTime.now().minusHours(JOB_RETENTION_HOURS);
        for (Map.Entry<String, ImportJobContext> entry : importJobs.entrySet()) {
            ImportJobContext context = entry.getValue();
            if (context == null || !isTerminal(context.getStatus())) {
                continue;
            }
            LocalDateTime finishedAt = context.getFinishedAt();
            if (finishedAt == null) {
                finishedAt = context.getCreatedAt();
            }
            if (finishedAt != null && finishedAt.isBefore(threshold)) {
                importJobs.remove(entry.getKey(), context);
            }
        }
    }

    private boolean isTerminal(ImportJobStatus status) {
        return status == ImportJobStatus.COMPLETED
                || status == ImportJobStatus.FAILED
                || status == ImportJobStatus.PARTIAL_SUCCESS
                || status == ImportJobStatus.CANCELLED;
    }

    private void markFinished(ImportJobContext context) {
        if (context != null && context.getFinishedAt() == null) {
            context.setFinishedAt(LocalDateTime.now());
        }
    }

    private void releaseTransientJobData(ImportJobContext context) {
        if (context == null) {
            return;
        }
        context.setHeaders(null);
        context.setData(null);
    }
    private static class ImportJobContext {
        private String jobId;
        private String tenantId;
        private String entityType;
        private String operator;
        private String fileName;
        private ImportJobStatus status;
        private int totalRows;
        private int processedRows;
        private int successCount;
        private int failCount;
        private String errorMessage;
        private List<String> headers;
        private List<Map<String, String>> data;
        private List<ImportError> errors;
        private LocalDateTime createdAt;
        private LocalDateTime finishedAt;

        // Getters and Setters
        public String getJobId() { return jobId; }
        public void setJobId(String jobId) { this.jobId = jobId; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getEntityType() { return entityType; }
        public void setEntityType(String entityType) { this.entityType = entityType; }
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public ImportJobStatus getStatus() { return status; }
        public void setStatus(ImportJobStatus status) { this.status = status; }
        public int getTotalRows() { return totalRows; }
        public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
        public int getProcessedRows() { return processedRows; }
        public void setProcessedRows(int processedRows) { this.processedRows = processedRows; }
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getFailCount() { return failCount; }
        public void setFailCount(int failCount) { this.failCount = failCount; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public List<String> getHeaders() { return headers; }
        public void setHeaders(List<String> headers) { this.headers = headers; }
        public List<Map<String, String>> getData() { return data; }
        public void setData(List<Map<String, String>> data) { this.data = data; }
        public List<ImportError> getErrors() { return errors; }
        public void setErrors(List<ImportError> errors) { this.errors = errors; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getFinishedAt() { return finishedAt; }
        public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    }

    /**
     * 导入错误信息
     */
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

    /**
     * 导入任务结果
     */
    public static class ImportJobResult {
        private String jobId;
        private String status;
        private int totalRows;
        private int processedRows;
        private int successCount;
        private int failCount;
        private String errorMessage;
        private List<ImportError> errors;

        public ImportJobResult(String jobId, String status, int totalRows, int processedRows,
                               int successCount, int failCount) {
            this.jobId = jobId;
            this.status = status;
            this.totalRows = totalRows;
            this.processedRows = processedRows;
            this.successCount = successCount;
            this.failCount = failCount;
        }

        public ImportJobResult(String jobId, String status, int totalRows, int processedRows,
                               int successCount, int failCount, String errorMessage) {
            this(jobId, status, totalRows, processedRows, successCount, failCount);
            this.errorMessage = errorMessage;
        }

        public ImportJobResult(String jobId, String status, int totalRows, int processedRows,
                               int successCount, int failCount, String errorMessage,
                               List<ImportError> errors) {
            this(jobId, status, totalRows, processedRows, successCount, failCount, errorMessage);
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
}
