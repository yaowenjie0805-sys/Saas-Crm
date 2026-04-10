package com.yao.crm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.entity.Product;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.ContactRepository;
import com.yao.crm.repository.LeadRepository;
import com.yao.crm.repository.ProductRepository;
import com.yao.crm.service.DataMappingService.EntityType;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 鏁版嵁瀵煎嚭鏈嶅姟
 * 璐熻矗瀵煎嚭涓氬姟閫昏緫銆佹枃浠剁敓鎴愩€佷换鍔＄鐞?
 */
@Service
@EnableAsync
@Slf4j
public class DataExportService {

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final int EXPORT_QUERY_PAGE_SIZE = 500;

    // 瀵煎嚭浠诲姟鐘舵€?
    public enum ExportJobStatus {
        PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    }

    private final CacheService cacheService;
    private final ObjectMapper objectMapper;
    private final DataMappingService dataMappingService;
    private final CustomerRepository customerRepository;
    private final ContactRepository contactRepository;
    private final LeadRepository leadRepository;
    private final ProductRepository productRepository;

    public DataExportService(
            CacheService cacheService,
            ObjectMapper objectMapper,
            DataMappingService dataMappingService,
            CustomerRepository customerRepository,
            ContactRepository contactRepository,
            LeadRepository leadRepository,
            ProductRepository productRepository) {
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
        this.dataMappingService = dataMappingService;
        this.customerRepository = customerRepository;
        this.contactRepository = contactRepository;
        this.leadRepository = leadRepository;
        this.productRepository = productRepository;
    }

    /**
     * 鍒涘缓瀵煎嚭浠诲姟
     */
    @Transactional(readOnly = true)
    public ExportJobResult createExportJob(String tenantId, String operator, String entityType,
                                          Map<String, Object> filters, List<String> fields,
                                          String format) {
        String jobId = UUID.randomUUID().toString();

        try {
            String requiredTenantId = requireNonBlank(tenantId, "tenant_id_required");
            String requiredOperator = requireNonBlank(operator, "operator_required");
            String normalizedFormat = normalizeRequestedFormat(format);
            EntityType type = EntityType.fromCode(entityType);
            ExportQueryData queryData = queryEntityData(requiredTenantId, type, filters);
            List<Map<String, Object>> filteredData = queryData.filteredData;
            List<String> selectedFields = resolveFields(queryData.firstRow, filteredData, fields);

            ExportJobContext context = new ExportJobContext();
            context.setJobId(jobId);
            context.setTenantId(requiredTenantId);
            context.setEntityType(entityType);
            context.setOperator(requiredOperator);
            context.setStatus(ExportJobStatus.PENDING);
            context.setTotalRows(filteredData.size());
            context.setFields(selectedFields);
            context.setData(filteredData);
            context.setFormat(normalizedFormat);
            context.setCreatedAt(LocalDateTime.now());

            cacheService.setExportJobContext(jobId, context);

            // 寮傛鐢熸垚鏂囦欢
            generateExportFileAsync(jobId);

            return new ExportJobResult(jobId, context.getStatus().name(), context.getTotalRows());

        } catch (Exception e) {
            log.error("Failed to create export job", e);
            return new ExportJobResult(null, ExportJobStatus.FAILED.name(), 0, e.getMessage());
        }
    }

    /**
     * 鏌ヨ瀹炰綋鏁版嵁
     */
    private ExportQueryData queryEntityData(String tenantId, EntityType entityType, Map<String, Object> filters) {
        ExportQueryData queryData = new ExportQueryData();

        switch (entityType) {
            case CUSTOMER:
                collectCustomers(tenantId, filters, queryData);
                break;
            case CONTACT:
                collectContacts(tenantId, filters, queryData);
                break;
            case LEAD:
                collectLeads(tenantId, filters, queryData);
                break;
            case PRODUCT:
                collectProducts(tenantId, filters, queryData);
                break;
            default:
                throw new IllegalArgumentException("Unsupported entity type for export: " + entityType);
        }
        return queryData;
    }

    private void collectCustomers(String tenantId, Map<String, Object> filters, ExportQueryData queryData) {
        int page = 0;
        Page<?> customerPage;
        do {
            Pageable pageable = PageRequest.of(page, EXPORT_QUERY_PAGE_SIZE);
            customerPage = customerRepository.findByTenantId(tenantId, pageable);
            collectPageRows(customerPage.getContent(), filters, queryData);
            page++;
        } while (customerPage.hasNext());
    }

    private void collectContacts(String tenantId, Map<String, Object> filters, ExportQueryData queryData) {
        int page = 0;
        Page<?> contactPage;
        do {
            Pageable pageable = PageRequest.of(page, EXPORT_QUERY_PAGE_SIZE);
            contactPage = contactRepository.findByTenantId(tenantId, pageable);
            collectPageRows(contactPage.getContent(), filters, queryData);
            page++;
        } while (contactPage.hasNext());
    }

    private void collectLeads(String tenantId, Map<String, Object> filters, ExportQueryData queryData) {
        int page = 0;
        Page<?> leadPage;
        do {
            Pageable pageable = PageRequest.of(page, EXPORT_QUERY_PAGE_SIZE);
            leadPage = leadRepository.findByTenantId(tenantId, pageable);
            collectPageRows(leadPage.getContent(), filters, queryData);
            page++;
        } while (leadPage.hasNext());
    }

    private void collectProducts(String tenantId, Map<String, Object> filters, ExportQueryData queryData) {
        int page = 0;
        Page<Product> productPage;
        do {
            Pageable pageable = PageRequest.of(page, EXPORT_QUERY_PAGE_SIZE);
            productPage = productRepository.findByTenantId(tenantId, pageable);
            collectPageRows(productPage.getContent(), filters, queryData);
            page++;
        } while (productPage.hasNext());
    }

    private void collectPageRows(List<?> entities, Map<String, Object> filters, ExportQueryData queryData) {
        for (Object entity : entities) {
            Map<String, Object> row = objectMapper.convertValue(entity, new TypeReference<Map<String, Object>>() {});
            if (queryData.firstRow == null && row != null && !row.isEmpty()) {
                queryData.firstRow = row;
            }
            if (matchesFilters(row, filters)) {
                queryData.filteredData.add(row);
            }
        }
    }
    /**
     * 寮傛鐢熸垚瀵煎嚭鏂囦欢
     */
    @Async
    public CompletableFuture<Void> generateExportFileAsync(String jobId) {
        Optional<ExportJobContext> optContext = cacheService.getExportJobContext(jobId, ExportJobContext.class);
        if (!optContext.isPresent()) {
            return CompletableFuture.completedFuture(null);
        }

        ExportJobContext context = optContext.get();
        context.setStatus(ExportJobStatus.RUNNING);
        cacheService.setExportJobContext(jobId, context);

        try {
            String normalizedFormat = normalizeStoredFormat(context.getFormat());
            context.setFormat(normalizedFormat);
            String fileName = generateFileName(context.getEntityType(), normalizedFormat);
            Path tempDir = Files.createTempDirectory("crm_export");
            Path filePath = tempDir.resolve(fileName);

            if ("xlsx".equalsIgnoreCase(normalizedFormat)) {
                generateExcelFile(context.getData(), context.getFields(), filePath);
            } else if ("csv".equalsIgnoreCase(normalizedFormat)) {
                generateCsvFile(context.getData(), context.getFields(), filePath);
            } else if ("json".equalsIgnoreCase(normalizedFormat)) {
                generateJsonFile(context.getData(), filePath);
            }

            context.setFilePath(filePath.toString());
            context.setStatus(ExportJobStatus.COMPLETED);
            context.setCompletedAt(LocalDateTime.now());
            context.setData(null);
            context.setFields(null);

            log.info("Export file generated: {}", filePath);
            cacheService.setExportJobContext(jobId, context);

        } catch (Exception e) {
            log.error("Export job failed: jobId={}", jobId, e);
            context.setStatus(ExportJobStatus.FAILED);
            context.setErrorMessage(e.getMessage());
            context.setData(null);
            context.setFields(null);
            cacheService.setExportJobContext(jobId, context);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 鐢熸垚Excel鏂囦欢
     */
    public void generateExcelFile(List<Map<String, Object>> data, List<String> fields, Path filePath)
            throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Export Data");

            // 鍒涘缓琛ㄥご鏍峰紡
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // 鍐欏叆琛ㄥご
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < fields.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(fields.get(i));
                cell.setCellStyle(headerStyle);
            }

            // 鍐欏叆鏁版嵁
            for (int i = 0; i < data.size(); i++) {
                Row row = sheet.createRow(i + 1);
                Map<String, Object> record = data.get(i);
                for (int j = 0; j < fields.size(); j++) {
                    Cell cell = row.createCell(j);
                    Object value = record.get(fields.get(j));
                    if (value != null) {
                        cell.setCellValue(value.toString());
                    }
                }
            }

            // 鑷姩璋冩暣鍒楀
            boolean shouldAutoSize = data.size() <= 2000 && fields.size() <= 50;
            for (int i = 0; i < fields.size(); i++) {
                if (shouldAutoSize) {
                    sheet.autoSizeColumn(i);
                } else {
                    sheet.setColumnWidth(i, 20 * 256);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                workbook.write(fos);
            }
        }
    }

    /**
     * 鐢熸垚CSV鏂囦欢
     */
    public void generateCsvFile(List<Map<String, Object>> data, List<String> fields, Path filePath)
            throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            // 鍐欏叆BOM锛堣В鍐矱xcel鎵撳紑UTF-8 CSV涔辩爜闂锛?
            writer.write('\uFEFF');

            // 鍐欏叆琛ㄥご
            writer.write(String.join(",", fields));
            writer.newLine();

            // 鍐欏叆鏁版嵁
            for (Map<String, Object> record : data) {
                List<String> row = new ArrayList<>();
                for (String field : fields) {
                    Object value = record.get(field);
                    row.add(dataMappingService.escapeCsvValue(value != null ? value.toString() : ""));
                }
                writer.write(String.join(",", row));
                writer.newLine();
            }
        }
    }

    /**
     * 鐢熸垚JSON鏂囦欢
     */
    public void generateJsonFile(List<Map<String, Object>> data, Path filePath)
            throws JsonProcessingException, IOException {
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        // Java 8 鍏煎锛氫娇鐢?BufferedWriter 浠ｆ浛 Files.writeString
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write(json);
        }
    }

    /**
     * 鐢熸垚鏂囦欢鍚?
     */
    private String generateFileName(String entityType, String format) {
        String prefix = EntityType.fromCode(entityType).getName();
        String timestamp = LocalDateTime.now().format(FILE_DATE_FORMAT);
        return prefix + "_export_" + timestamp + "." + normalizeStoredFormat(format);
    }

    /**
     * 鑾峰彇瀵煎嚭浠诲姟鐘舵€?
     */
    public ExportJobResult getExportJobStatus(String tenantId, String operator, String jobId, boolean canViewAll) {
        Optional<ExportJobContext> optContext = cacheService.getExportJobContext(jobId, ExportJobContext.class);
        if (!optContext.isPresent()) {
            return null;
        }
        ExportJobContext context = optContext.get();
        assertJobAccessible(context, tenantId, operator, canViewAll);
        if (context.getFilePath() != null) {
            Path path = resolveSafePath(context.getFilePath());
            if (path == null || !Files.exists(path)) {
                context.setFilePath(null);
                cacheService.setExportJobContext(jobId, context);
            }
        }
        return new ExportJobResult(
                jobId,
                context.getStatus().name(),
                context.getTotalRows(),
                context.getFilePath(),
                context.getErrorMessage()
        );
    }

    /**
     * 鑾峰彇瀵煎嚭鏂囦欢
     */
    public byte[] getExportFile(String tenantId, String operator, String jobId, boolean canViewAll) throws IOException {
        Optional<ExportJobContext> optContext = cacheService.getExportJobContext(jobId, ExportJobContext.class);
        if (!optContext.isPresent() || optContext.get().getFilePath() == null) {
            return null;
        }
        ExportJobContext context = optContext.get();
        assertJobAccessible(context, tenantId, operator, canViewAll);
        Path filePath = resolveSafePath(context.getFilePath());
        if (filePath == null || !Files.exists(filePath)) {
            context.setFilePath(null);
            cacheService.setExportJobContext(jobId, context);
            return null;
        }
        byte[] content = Files.readAllBytes(filePath);
        try {
            Files.deleteIfExists(filePath);
            context.setFilePath(null);
            cacheService.setExportJobContext(jobId, context);
        } catch (IOException ex) {
            log.warn("Failed to cleanup export temp file: {}", filePath, ex);
        }
        return content;
    }

    private Path resolveSafePath(String rawPath) {
        if (rawPath == null || rawPath.trim().isEmpty()) {
            return null;
        }
        try {
            return Paths.get(rawPath);
        } catch (InvalidPathException ex) {
            return null;
        }
    }

    /**
     * 鑾峰彇瀵煎叆妯℃澘
     */
    public byte[] getImportTemplate(String entityType, String format) throws IOException {
        EntityType type = EntityType.fromCode(entityType);
        List<String> headers = dataMappingService.getTemplateHeaders(type);
        List<Map<String, Object>> sampleData = dataMappingService.getSampleData(type);
        String templateFormat = normalizeTemplateFormat(format);

        if ("xlsx".equalsIgnoreCase(templateFormat)) {
            Path tempFile = Files.createTempFile("template", ".xlsx");
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Import Template");

                // 琛ㄥご
                Row headerRow = sheet.createRow(0);
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerFont.setColor(IndexedColors.WHITE.getIndex());
                headerStyle.setFont(headerFont);
                headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers.get(i));
                    cell.setCellStyle(headerStyle);
                }

                // 绀轰緥鏁版嵁
                for (int i = 0; i < sampleData.size(); i++) {
                    Row row = sheet.createRow(i + 1);
                    Map<String, Object> record = sampleData.get(i);
                    List<String> fields = dataMappingService.getTemplateFields(type);
                    for (int j = 0; j < fields.size(); j++) {
                        Cell cell = row.createCell(j);
                        Object value = record.get(fields.get(j));
                        if (value != null) {
                            cell.setCellValue(value.toString());
                        }
                    }
                }

                for (int i = 0; i < headers.size(); i++) {
                    sheet.autoSizeColumn(i);
                }

                try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
                    workbook.write(fos);
                }
            }
            return Files.readAllBytes(tempFile);
        } else {
            // CSV鏍煎紡
            StringBuilder csv = new StringBuilder();
            csv.append('\uFEFF'); // BOM
            csv.append(String.join(",", headers)).append("\n");

            for (Map<String, Object> record : sampleData) {
                List<String> fields = dataMappingService.getTemplateFields(type);
                List<String> row = new ArrayList<>();
                for (String field : fields) {
                    Object value = record.get(field);
                    row.add(dataMappingService.escapeCsvValue(value != null ? value.toString() : ""));
                }
                csv.append(String.join(",", row)).append("\n");
            }
            return csv.toString().getBytes(StandardCharsets.UTF_8);
        }
    }

    private String requireNonBlank(String value, String errorCode) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(errorCode);
        }
        return normalized;
    }

    private String normalizeRequestedFormat(String format) {
        String normalized = format == null ? "" : format.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return "csv";
        }
        if ("csv".equals(normalized) || "xlsx".equals(normalized) || "json".equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("unsupported_export_format");
    }

    private String normalizeStoredFormat(String format) {
        String normalized = format == null ? "" : format.trim().toLowerCase();
        if ("csv".equals(normalized) || "xlsx".equals(normalized) || "json".equals(normalized)) {
            return normalized;
        }
        return "csv";
    }

    private String normalizeTemplateFormat(String format) {
        String normalized = format == null ? "" : format.trim().toLowerCase();
        if ("csv".equals(normalized) || "xlsx".equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("unsupported_template_format");
    }

    private boolean matchesFilters(Map<String, Object> row, Map<String, Object> filters) {
        if (row == null) {
            return false;
        }
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.trim().isEmpty()) {
                continue;
            }
            Object expected = entry.getValue();
            if (expected == null) {
                continue;
            }
            Object actual = row.get(key);
            String expectedText = String.valueOf(expected).trim();
            if (expectedText.isEmpty()) {
                continue;
            }
            if (actual == null) {
                return false;
            }
            if (!expectedText.equalsIgnoreCase(String.valueOf(actual).trim())) {
                return false;
            }
        }
        return true;
    }

    private List<String> resolveFields(Map<String, Object> firstRow, List<Map<String, Object>> filteredData, List<String> fields) {
        if (fields != null && !fields.isEmpty()) {
            return fields;
        }
        Map<String, Object> source = firstRow;
        if ((source == null || source.isEmpty()) && filteredData != null && !filteredData.isEmpty()) {
            source = filteredData.get(0);
        }
        if (source == null || source.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> inferred = new LinkedHashSet<>(source.keySet());
        return new ArrayList<>(inferred);
    }

    private void assertJobAccessible(ExportJobContext context, String tenantId, String operator, boolean canViewAll) {
        String requiredTenant = requireNonBlank(tenantId, "tenant_id_required");
        String requiredOperator = requireNonBlank(operator, "operator_required");
        if (!requiredTenant.equals(context.getTenantId())) {
            throw new IllegalArgumentException("forbidden");
        }
        if (!canViewAll && !requiredOperator.equals(context.getOperator())) {
            throw new IllegalArgumentException("forbidden");
        }
    }

    private static class ExportQueryData {
        private Map<String, Object> firstRow;
        private final List<Map<String, Object>> filteredData = new ArrayList<>();
    }

    /**
     * 瀵煎嚭浠诲姟涓婁笅鏂?
     */
    private static class ExportJobContext {
        private String jobId;
        private String tenantId;
        private String entityType;
        private String operator;
        private ExportJobStatus status;
        private int totalRows;
        private List<String> fields;
        private List<Map<String, Object>> data;
        private String format;
        private String filePath;
        private String errorMessage;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;

        // Getters and Setters
        public String getJobId() { return jobId; }
        public void setJobId(String jobId) { this.jobId = jobId; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getEntityType() { return entityType; }
        public void setEntityType(String entityType) { this.entityType = entityType; }
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
        public ExportJobStatus getStatus() { return status; }
        public void setStatus(ExportJobStatus status) { this.status = status; }
        public int getTotalRows() { return totalRows; }
        public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
        public List<String> getFields() { return fields; }
        public void setFields(List<String> fields) { this.fields = fields; }
        public List<Map<String, Object>> getData() { return data; }
        public void setData(List<Map<String, Object>> data) { this.data = data; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    }

    /**
     * 瀵煎嚭浠诲姟缁撴灉
     */
    public static class ExportJobResult {
        private String jobId;
        private String status;
        private int totalRows;
        private String filePath;
        private String errorMessage;

        public ExportJobResult(String jobId, String status, int totalRows) {
            this.jobId = jobId;
            this.status = status;
            this.totalRows = totalRows;
        }

        public ExportJobResult(String jobId, String status, int totalRows, String errorMessage) {
            this(jobId, status, totalRows);
            this.errorMessage = errorMessage;
        }

        public ExportJobResult(String jobId, String status, int totalRows, String filePath, String errorMessage) {
            this(jobId, status, totalRows, errorMessage);
            this.filePath = filePath;
        }

        // Getters
        public String getJobId() { return jobId; }
        public String getStatus() { return status; }
        public int getTotalRows() { return totalRows; }
        public String getFilePath() { return filePath; }
        public String getErrorMessage() { return errorMessage; }
    }
}
