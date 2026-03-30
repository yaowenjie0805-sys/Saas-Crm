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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 数据导出服务
 * 负责导出业务逻辑、文件生成、任务管理
 */
@Service
@Slf4j
public class DataExportService {

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // 导出任务状态
    public enum ExportJobStatus {
        PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    }

    // 导出任务缓存
    private final Map<String, ExportJobContext> exportJobs = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;
    private final DataMappingService dataMappingService;
    private final CustomerRepository customerRepository;
    private final ContactRepository contactRepository;
    private final LeadRepository leadRepository;
    private final ProductRepository productRepository;

    public DataExportService(
            ObjectMapper objectMapper,
            DataMappingService dataMappingService,
            CustomerRepository customerRepository,
            ContactRepository contactRepository,
            LeadRepository leadRepository,
            ProductRepository productRepository) {
        this.objectMapper = objectMapper;
        this.dataMappingService = dataMappingService;
        this.customerRepository = customerRepository;
        this.contactRepository = contactRepository;
        this.leadRepository = leadRepository;
        this.productRepository = productRepository;
    }

    /**
     * 创建导出任务
     */
    @Transactional(readOnly = true)
    public ExportJobResult createExportJob(String tenantId, String operator, String entityType,
                                          Map<String, Object> filters, List<String> fields,
                                          String format) {
        String jobId = UUID.randomUUID().toString();

        try {
            EntityType type = EntityType.fromCode(entityType);
            List<Map<String, Object>> data = queryEntityData(tenantId, type, filters, fields);

            ExportJobContext context = new ExportJobContext();
            context.setJobId(jobId);
            context.setTenantId(tenantId);
            context.setEntityType(entityType);
            context.setOperator(operator);
            context.setStatus(ExportJobStatus.PENDING);
            context.setTotalRows(data.size());
            context.setFields(fields);
            context.setData(data);
            context.setFormat(format);
            context.setCreatedAt(LocalDateTime.now());

            exportJobs.put(jobId, context);

            // 异步生成文件
            generateExportFileAsync(jobId);

            return new ExportJobResult(jobId, context.getStatus().name(), context.getTotalRows());

        } catch (Exception e) {
            log.error("Failed to create export job", e);
            return new ExportJobResult(null, ExportJobStatus.FAILED.name(), 0, e.getMessage());
        }
    }

    /**
     * 查询实体数据
     */
    private List<Map<String, Object>> queryEntityData(String tenantId, EntityType entityType,
                                                      Map<String, Object> filters,
                                                      List<String> fields) {
        List<?> entities;

        switch (entityType) {
            case CUSTOMER:
                entities = customerRepository.findByTenantId(tenantId);
                break;
            case CONTACT:
                entities = contactRepository.findByTenantId(tenantId);
                break;
            case LEAD:
                entities = leadRepository.findByTenantId(tenantId);
                break;
            case PRODUCT:
                entities = fetchAllProducts(tenantId);
                break;
            default:
                throw new IllegalArgumentException("Unsupported entity type for export: " + entityType);
        }

        // 转换为Map列表
        return entities.stream()
                .map(entity -> objectMapper.convertValue(entity,
                        new TypeReference<Map<String, Object>>() {}))
                .collect(Collectors.toList());
    }

    /**
     * 分页获取所有产品（兼容Java 8 Pageable）
     */
    private List<Product> fetchAllProducts(String tenantId) {
        List<Product> allProducts = new ArrayList<>();
        int page = 0;
        int pageSize = 100;
        Page<Product> productPage;
        do {
            Pageable pageable = PageRequest.of(page, pageSize);
            productPage = productRepository.findByTenantId(tenantId, pageable);
            allProducts.addAll(productPage.getContent());
            page++;
        } while (productPage.hasNext());
        return allProducts;
    }

    /**
     * 异步生成导出文件
     */
    private void generateExportFileAsync(String jobId) {
        ExportJobContext context = exportJobs.get(jobId);
        if (context == null) return;

        context.setStatus(ExportJobStatus.RUNNING);

        try {
            String fileName = generateFileName(context.getEntityType(), context.getFormat());
            Path tempDir = Files.createTempDirectory("crm_export");
            Path filePath = tempDir.resolve(fileName);

            if ("xlsx".equalsIgnoreCase(context.getFormat())) {
                generateExcelFile(context.getData(), context.getFields(), filePath);
            } else if ("csv".equalsIgnoreCase(context.getFormat())) {
                generateCsvFile(context.getData(), context.getFields(), filePath);
            } else if ("json".equalsIgnoreCase(context.getFormat())) {
                generateJsonFile(context.getData(), filePath);
            }

            context.setFilePath(filePath.toString());
            context.setStatus(ExportJobStatus.COMPLETED);
            context.setCompletedAt(LocalDateTime.now());

            log.info("Export file generated: {}", filePath);

        } catch (Exception e) {
            log.error("Export job failed: jobId={}", jobId, e);
            context.setStatus(ExportJobStatus.FAILED);
            context.setErrorMessage(e.getMessage());
        }
    }

    /**
     * 生成Excel文件
     */
    public void generateExcelFile(List<Map<String, Object>> data, List<String> fields, Path filePath)
            throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Export Data");

            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // 写入表头
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < fields.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(fields.get(i));
                cell.setCellStyle(headerStyle);
            }

            // 写入数据
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

            // 自动调整列宽
            for (int i = 0; i < fields.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                workbook.write(fos);
            }
        }
    }

    /**
     * 生成CSV文件
     */
    public void generateCsvFile(List<Map<String, Object>> data, List<String> fields, Path filePath)
            throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            // 写入BOM（解决Excel打开UTF-8 CSV乱码问题）
            writer.write('\uFEFF');

            // 写入表头
            writer.write(String.join(",", fields));
            writer.newLine();

            // 写入数据
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
     * 生成JSON文件
     */
    public void generateJsonFile(List<Map<String, Object>> data, Path filePath)
            throws JsonProcessingException, IOException {
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        // Java 8 兼容：使用 BufferedWriter 代替 Files.writeString
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write(json);
        }
    }

    /**
     * 生成文件名
     */
    private String generateFileName(String entityType, String format) {
        String prefix = EntityType.fromCode(entityType).getName();
        String timestamp = LocalDateTime.now().format(FILE_DATE_FORMAT);
        return prefix + "_导出_" + timestamp + "." + format.toLowerCase();
    }

    /**
     * 获取导出任务状态
     */
    public ExportJobResult getExportJobStatus(String jobId) {
        ExportJobContext context = exportJobs.get(jobId);
        if (context == null) {
            return null;
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
     * 获取导出文件
     */
    public byte[] getExportFile(String jobId) throws IOException {
        ExportJobContext context = exportJobs.get(jobId);
        if (context == null || context.getFilePath() == null) {
            return null;
        }
        Path filePath = Paths.get(context.getFilePath());
        if (!Files.exists(filePath)) {
            return null;
        }
        return Files.readAllBytes(filePath);
    }

    /**
     * 获取导入模板
     */
    public byte[] getImportTemplate(String entityType, String format) throws IOException {
        EntityType type = EntityType.fromCode(entityType);
        List<String> headers = dataMappingService.getTemplateHeaders(type);
        List<Map<String, Object>> sampleData = dataMappingService.getSampleData(type);

        if ("xlsx".equalsIgnoreCase(format)) {
            Path tempFile = Files.createTempFile("template", ".xlsx");
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Import Template");

                // 表头
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

                // 示例数据
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
            // CSV格式
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

    /**
     * 导出任务上下文
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
     * 导出任务结果
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
