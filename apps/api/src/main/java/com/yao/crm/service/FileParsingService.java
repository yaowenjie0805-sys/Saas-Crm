package com.yao.crm.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文件解析服务
 * 负责 CSV、Excel、JSON 等文件的解析
 */
@Service
@Slf4j
public class FileParsingService {

    private final ObjectMapper objectMapper;

    public FileParsingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析导入文件
     */
    public ParsedData parseFile(InputStream inputStream, String fileExtension, String entityType) throws IOException {
        ParsedData data;

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
    public ParsedData parseExcel(InputStream inputStream) throws IOException {
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
    public ParsedData parseCsv(InputStream inputStream) throws IOException {
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
    public ParsedData parseJson(InputStream inputStream) throws IOException {
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
    public String[] splitCsvLine(String line) {
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
    public String getCellValue(Cell cell) {
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
     * 解析后的数据结构
     */
    public static class ParsedData {
        private List<String> headers = new ArrayList<>();
        private List<Map<String, String>> rows = new ArrayList<>();

        public List<String> getHeaders() { return headers; }
        public void setHeaders(List<String> headers) { this.headers = headers; }
        public List<Map<String, String>> getRows() { return rows; }
        public void setRows(List<Map<String, String>> rows) { this.rows = rows; }
    }
}
