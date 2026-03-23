package com.yao.crm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.entity.*;
import com.yao.crm.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 通用数据导入导出服务
 * 支持多种实体的批量导入导出，包括客户、联系人、线索、商机、产品等
 */
@Service
public class DataImportExportService {

    private static final Logger log = LoggerFactory.getLogger(DataImportExportService.class);
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ObjectMapper objectMapper;
    private final CustomerRepository customerRepository;
    private final ContactRepository contactRepository;
    private final LeadRepository leadRepository;
    private final ProductRepository productRepository;

    // 支持的实体类型
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
            for (EntityType type : values()) {
                if (type.code.equalsIgnoreCase(code)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown entity type: " + code);
        }
    }

    // 导入任务状态
    public enum ImportJobStatus {
        PENDING, RUNNING, COMPLETED, FAILED, PARTIAL_SUCCESS, CANCELLED
    }

    // 导出任务状态
    public enum ExportJobStatus {
        PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    }

    // 导入任务缓存
    private final Map<String, ImportJobContext> importJobs = new ConcurrentHashMap<>();
    // 导出任务缓存
    private final Map<String, ExportJobContext> exportJobs = new ConcurrentHashMap<>();

    public DataImportExportService(
            ObjectMapper objectMapper,
            CustomerRepository customerRepository,
            ContactRepository contactRepository,
            LeadRepository leadRepository,
            ProductRepository productRepository) {
        this.objectMapper = objectMapper;
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
        String jobId = UUID.randomUUID().toString();

        try {
            // 解析文件
            ParsedData parsedData = parseFile(inputStream, fileExtension, entityType);

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
     * 解析导入文件
     */
    private ParsedData parseFile(InputStream inputStream, String fileExtension, String entityType) throws IOException {
        ParsedData data = new ParsedData();

        if ("xlsx".equalsIgnoreCase(fileExtension) || "xls".equalsIgnoreCase(fileExtension)) {
            data = parseExcel(inputStream);
        } else if ("csv".equalsIgnoreCase(fileExtension) || "txt".equalsIgnoreCase(fileExtension)) {
            data = parseCsv(inputStream);
        } else if ("json".equalsIgnoreCase(fileExtension)) {
            data = parseJson(inputStream);
        } else {
            throw new IllegalArgumentException("Unsupported file format: " + fileExtension);
        }

        return data;
    }

    /**
     * 解析Excel文件
     */
    private ParsedData parseExcel(InputStream inputStream) throws IOException {
        ParsedData data = new ParsedData();
        List<String> headers = new ArrayList<>();
        List<Map<String, String>> rows = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            boolean isFirstRow = true;

            for (Row row : sheet) {
                if (isFirstRow) {
                    // 读取表头
                    for (Cell cell : row) {
                        headers.add(getCellValue(cell));
                    }
                    isFirstRow = false;
                } else {
                    // 读取数据行
                    Map<String, String> rowData = new LinkedHashMap<>();
                    for (int i = 0; i < headers.size(); i++) {
                        Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        rowData.put(headers.get(i), getCellValue(cell));
                    }
                    rows.add(rowData);
                }
            }
        }

        data.setHeaders(headers);
        data.setRows(rows);
        return data;
    }

    /**
     * 解析CSV文件
     */
    private ParsedData parseCsv(InputStream inputStream) throws IOException {
        ParsedData data = new ParsedData();
        List<String> headers = new ArrayList<>();
        List<Map<String, String>> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            boolean isFirstRow = true;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] columns = splitCsvLine(line);

                if (isFirstRow) {
                    headers.addAll(Arrays.asList(columns));
                    isFirstRow = false;
                } else {
                    Map<String, String> rowData = new LinkedHashMap<>();
                    for (int i = 0; i < headers.size() && i < columns.length; i++) {
                        rowData.put(headers.get(i), columns[i].trim());
                    }
                    rows.add(rowData);
                }
            }
        }

        data.setHeaders(headers);
        data.setRows(rows);
        return data;
    }

    /**
     * 解析JSON文件
     */
    private ParsedData parseJson(InputStream inputStream) throws IOException {
        ParsedData data = new ParsedData();
        List<Map<String, String>> rows = new ArrayList<>();

        // 读取全部内容
        String content = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));

        // 解析为对象列表
        List<Map<String, Object>> jsonData = objectMapper.readValue(content,
                new TypeReference<List<Map<String, Object>>>() {});

        if (!jsonData.isEmpty()) {
            // 提取表头
            List<String> headers = new ArrayList<>(jsonData.get(0).keySet());
            data.setHeaders(headers);

            // 转换数据行
            for (Map<String, Object> jsonRow : jsonData) {
                Map<String, String> rowData = new LinkedHashMap<>();
                for (String header : headers) {
                    Object value = jsonRow.get(header);
                    rowData.put(header, value != null ? value.toString() : "");
                }
                rows.add(rowData);
            }
        }

        data.setRows(rows);
        return data;
    }

    /**
     * CSV行分割（处理引号）
     */
    private String[] splitCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                inQuote = !inQuote;
                continue;
            }
            if (ch == ',' && !inQuote) {
                out.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        out.add(current.toString().trim());
        return out.toArray(new String[0]);
    }

    /**
     * 获取Excel单元格值
     */
    private String getCellValue(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                double numValue = cell.getNumericCellValue();
                return numValue == Math.floor(numValue) ?
                        String.valueOf((long) numValue) :
                        String.valueOf(numValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
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

                try {
                    Map<String, String> row = data.get(i);
                    Object entity = mapRowToEntity(row, headers, entityType, context.getTenantId());
                    saveEntity(entity, entityType);
                    context.setSuccessCount(context.getSuccessCount() + 1);
                } catch (Exception e) {
                    context.setFailCount(context.getFailCount() + 1);
                    ImportError error = new ImportError();
                    error.setRowNumber(i + 2); // Excel行号从1开始，标题行是1
                    error.setErrorMessage(e.getMessage());
                    error.setRawData(rowToString(data.get(i)));
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

        } catch (Exception e) {
            log.error("Import job failed: " + jobId, e);
            context.setStatus(ImportJobStatus.FAILED);
            context.setErrorMessage(e.getMessage());
        }
    }

    /**
     * 将行数据映射为实体
     */
    private Object mapRowToEntity(Map<String, String> row, List<String> headers,
                                  EntityType entityType, String tenantId) {
        switch (entityType) {
            case CUSTOMER:
                return mapToCustomer(row, tenantId);
            case CONTACT:
                return mapToContact(row, tenantId);
            case LEAD:
                return mapToLead(row, tenantId);
            case PRODUCT:
                return mapToProduct(row, tenantId);
            default:
                throw new IllegalArgumentException("Unsupported entity type for import: " + entityType);
        }
    }

    private Customer mapToCustomer(Map<String, String> row, String tenantId) {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID().toString());
        customer.setTenantId(tenantId);
        customer.setName(row.getOrDefault("客户名称", row.getOrDefault("name", "")));
        customer.setIndustry(row.getOrDefault("行业", row.getOrDefault("industry", "")));
        customer.setScale(row.getOrDefault("规模", row.getOrDefault("scale", "")));
        customer.setPhone(row.getOrDefault("电话", row.getOrDefault("phone", "")));
        customer.setWebsite(row.getOrDefault("网站", row.getOrDefault("website", "")));
        customer.setAddress(row.getOrDefault("地址", row.getOrDefault("address", "")));
        customer.setDescription(row.getOrDefault("描述", row.getOrDefault("description", "")));
        customer.setStatus(row.getOrDefault("状态", row.getOrDefault("status", "ACTIVE")));
        return customer;
    }

    private Contact mapToContact(Map<String, String> row, String tenantId) {
        Contact contact = new Contact();
        contact.setId(UUID.randomUUID().toString());
        contact.setTenantId(tenantId);
        contact.setName(row.getOrDefault("姓名", row.getOrDefault("name", "")));
        contact.setEmail(row.getOrDefault("邮箱", row.getOrDefault("email", "")));
        contact.setPhone(row.getOrDefault("电话", row.getOrDefault("phone", "")));
        contact.setMobile(row.getOrDefault("手机", row.getOrDefault("mobile", "")));
        contact.setPosition(row.getOrDefault("职位", row.getOrDefault("position", "")));
        contact.setCompany(row.getOrDefault("公司", row.getOrDefault("company", "")));
        return contact;
    }

    private Lead mapToLead(Map<String, String> row, String tenantId) {
        Lead lead = new Lead();
        lead.setId(UUID.randomUUID().toString());
        lead.setTenantId(tenantId);
        lead.setName(row.getOrDefault("姓名", row.getOrDefault("name", "")));
        lead.setCompany(row.getOrDefault("公司", row.getOrDefault("company", "")));
        lead.setPhone(row.getOrDefault("电话", row.getOrDefault("phone", "")));
        lead.setEmail(row.getOrDefault("邮箱", row.getOrDefault("email", "")));
        lead.setSource(row.getOrDefault("来源", row.getOrDefault("source", "")));
        lead.setStatus(row.getOrDefault("状态", row.getOrDefault("status", "NEW")));
        return lead;
    }

    private Product mapToProduct(Map<String, String> row, String tenantId) {
        Product product = new Product();
        product.setId(UUID.randomUUID().toString());
        product.setTenantId(tenantId);
        product.setName(row.getOrDefault("产品名称", row.getOrDefault("name", "")));
        product.setCode(row.getOrDefault("产品代码", row.getOrDefault("code", "")));
        product.setCategory(row.getOrDefault("分类", row.getOrDefault("category", "")));
        product.setUnit(row.getOrDefault("单位", row.getOrDefault("unit", "个")));
        product.setStatus(row.getOrDefault("状态", row.getOrDefault("status", "ACTIVE")));

        // 价格处理
        String priceStr = row.getOrDefault("价格", row.getOrDefault("price", "0"));
        try {
            product.setPrice(java.math.BigDecimal.valueOf(Double.parseDouble(priceStr)));
        } catch (NumberFormatException e) {
            product.setPrice(java.math.BigDecimal.ZERO);
        }

        return product;
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
        ImportJobContext context = importJobs.get(jobId);
        if (context != null && (context.getStatus() == ImportJobStatus.PENDING ||
                context.getStatus() == ImportJobStatus.RUNNING)) {
            context.setStatus(ImportJobStatus.CANCELLED);
        }
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
            log.error("Export job failed: " + jobId, e);
            context.setStatus(ExportJobStatus.FAILED);
            context.setErrorMessage(e.getMessage());
        }
    }

    /**
     * 生成Excel文件
     */
    private void generateExcelFile(List<Map<String, Object>> data, List<String> fields, Path filePath)
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
    private void generateCsvFile(List<Map<String, Object>> data, List<String> fields, Path filePath)
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
                    row.add(escapeCsvValue(value != null ? value.toString() : ""));
                }
                writer.write(String.join(",", row));
                writer.newLine();
            }
        }
    }

    /**
     * 转义CSV值
     */
    private String escapeCsvValue(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * 生成JSON文件
     */
    private void generateJsonFile(List<Map<String, Object>> data, Path filePath)
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
        List<String> headers = getTemplateHeaders(type);
        List<Map<String, Object>> sampleData = getSampleData(type);

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
                    List<String> fields = getTemplateFields(type);
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
            csv.append("\uFEFF"); // BOM
            csv.append(String.join(",", headers)).append("\n");

            for (Map<String, Object> record : sampleData) {
                List<String> fields = getTemplateFields(type);
                List<String> row = new ArrayList<>();
                for (String field : fields) {
                    Object value = record.get(field);
                    row.add(escapeCsvValue(value != null ? value.toString() : ""));
                }
                csv.append(String.join(",", row)).append("\n");
            }
            return csv.toString().getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * 获取模板表头
     */
    private List<String> getTemplateHeaders(EntityType type) {
        return getTemplateFields(type).stream()
                .map(this::getFieldDisplayName)
                .collect(Collectors.toList());
    }

    /**
     * 获取模板字段列表
     */
    private List<String> getTemplateFields(EntityType type) {
        switch (type) {
            case CUSTOMER:
                return Arrays.asList("name", "industry", "scale", "phone", "website", "address", "status");
            case CONTACT:
                return Arrays.asList("name", "email", "phone", "mobile", "position", "company");
            case LEAD:
                return Arrays.asList("name", "company", "phone", "email", "source", "status");
            case PRODUCT:
                return Arrays.asList("name", "code", "category", "unit", "price", "status");
            default:
                return new ArrayList<>();
        }
    }

    /**
     * 获取字段显示名称
     */
    private String getFieldDisplayName(String field) {
        Map<String, String> fieldNames = new HashMap<>();
        fieldNames.put("name", "名称");
        fieldNames.put("industry", "行业");
        fieldNames.put("scale", "规模");
        fieldNames.put("phone", "电话");
        fieldNames.put("website", "网站");
        fieldNames.put("address", "地址");
        fieldNames.put("status", "状态");
        fieldNames.put("email", "邮箱");
        fieldNames.put("mobile", "手机");
        fieldNames.put("position", "职位");
        fieldNames.put("company", "公司");
        fieldNames.put("source", "来源");
        fieldNames.put("code", "代码");
        fieldNames.put("category", "分类");
        fieldNames.put("unit", "单位");
        fieldNames.put("price", "价格");
        return fieldNames.getOrDefault(field, field);
    }

    /**
     * 获取示例数据
     */
    private List<Map<String, Object>> getSampleData(EntityType type) {
        List<Map<String, Object>> samples = new ArrayList<>();

        switch (type) {
            case CUSTOMER:
                Map<String, Object> customer = new LinkedHashMap<>();
                customer.put("name", "示例公司");
                customer.put("industry", "互联网");
                customer.put("scale", "100-500人");
                customer.put("phone", "010-12345678");
                customer.put("website", "www.example.com");
                customer.put("address", "北京市朝阳区");
                customer.put("status", "ACTIVE");
                samples.add(customer);
                break;

            case CONTACT:
                Map<String, Object> contact = new LinkedHashMap<>();
                contact.put("name", "张三");
                contact.put("email", "zhangsan@example.com");
                contact.put("phone", "010-12345678");
                contact.put("mobile", "13800138000");
                contact.put("position", "经理");
                contact.put("company", "示例公司");
                samples.add(contact);
                break;

            case LEAD:
                Map<String, Object> lead = new LinkedHashMap<>();
                lead.put("name", "李四");
                lead.put("company", "测试公司");
                lead.put("phone", "010-87654321");
                lead.put("email", "lisi@example.com");
                lead.put("source", "官网");
                lead.put("status", "NEW");
                samples.add(lead);
                break;

            case PRODUCT:
                Map<String, Object> product = new LinkedHashMap<>();
                product.put("name", "示例产品");
                product.put("code", "P001");
                product.put("category", "软件");
                product.put("unit", "套");
                product.put("price", "9999.00");
                product.put("status", "ACTIVE");
                samples.add(product);
                break;
        }

        return samples;
    }

    /**
     * 工具方法：将行数据转为字符串
     */
    private String rowToString(Map<String, String> row) {
        return row.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", "));
    }

    // ========== 内部类和结果类 ==========

    private static class ParsedData {
        private List<String> headers = new ArrayList<>();
        private List<Map<String, String>> rows = new ArrayList<>();

        public List<String> getHeaders() { return headers; }
        public void setHeaders(List<String> headers) { this.headers = headers; }
        public List<Map<String, String>> getRows() { return rows; }
        public void setRows(List<Map<String, String>> rows) { this.rows = rows; }
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
    }

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
