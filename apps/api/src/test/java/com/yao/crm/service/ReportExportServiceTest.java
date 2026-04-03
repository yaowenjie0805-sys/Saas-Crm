package com.yao.crm.service;

import static com.yao.crm.support.TestTenant.TENANT_TEST;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReportExportService
 */
@ExtendWith(MockitoExtension.class)
class ReportExportServiceTest {

    @Mock
    private ReportService reportService;

    private ReportExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = new ReportExportService(reportService);
    }

    @Test
    @DisplayName("shouldExportCsvWithCorrectFormat_whenExportOverviewCsv")
    void shouldExportCsvWithCorrectFormat_whenExportOverviewCsv() {
        // Arrange
        String tenantId = "tenant-1";
        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 1, 31);
        String role = "ADMIN";
        String owner = "";
        String department = "";

        Map<String, Object> mockReport = createMockReport();
        when(reportService.overviewByTenant(tenantId, fromDate, toDate, role, owner, department))
                .thenReturn(mockReport);

        // Act
        String csv = exportService.exportOverviewCsvByTenant(tenantId, fromDate, toDate, role, owner, department, "en");

        // Assert
        assertNotNull(csv);
        assertTrue(csv.startsWith("\uFEFF")); // BOM for Excel compatibility
        assertTrue(csv.contains("section,key,value")); // English header
        assertTrue(csv.contains("filter"));
        assertTrue(csv.contains("summary"));
        assertTrue(csv.contains("2026-01-01"));
        assertTrue(csv.contains("2026-01-31"));
        assertTrue(csv.contains("ADMIN"));
    }

    @Test
    @DisplayName("shouldExportEmptyReport_whenNoData")
    void shouldExportEmptyReport_whenNoData() {
        // Arrange
        String tenantId = "tenant-1";
        LocalDate fromDate = null;
        LocalDate toDate = null;
        String role = "";
        String owner = "";
        String department = "";

        Map<String, Object> emptyReport = createEmptyReport();
        when(reportService.overviewByTenant(tenantId, null, null, role, owner, department))
                .thenReturn(emptyReport);

        // Act
        String csv = exportService.exportOverviewCsvByTenant(tenantId, fromDate, toDate, role, owner, department, "en");

        // Assert
        assertNotNull(csv);
        assertTrue(csv.contains("section,key,value"));
        // Check that summary section exists even with zeros
        assertTrue(csv.contains("summary"));
    }

    @Test
    @DisplayName("shouldExportCsvInChinese_whenLanguageIsZh")
    void shouldExportCsvInChinese_whenLanguageIsZh() {
        // Arrange
        String tenantId = TENANT_TEST;
        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 1, 31);
        String role = "ADMIN";
        String owner = "";
        String department = "";
        String language = "zh-CN";

        Map<String, Object> mockReport = createMockReport();
        when(reportService.overviewByTenant(tenantId, fromDate, toDate, role, owner, department))
                .thenReturn(mockReport);

        // Act
        String csv = exportService.exportOverviewCsvByTenant(tenantId, fromDate, toDate, role, owner, department, language);

        // Assert
        assertNotNull(csv);
        assertTrue(csv.contains("\u5206\u7EC4,\u5B57\u6BB5,\u503C")); // Chinese header
        assertTrue(csv.contains("\u7B5B\u9009")); // filter in Chinese
        assertTrue(csv.contains("\u6C47\u603B")); // summary in Chinese
        assertTrue(csv.contains("\u5F00\u59CB")); // from in Chinese
        assertTrue(csv.contains("\u7ED3\u675F")); // to in Chinese
    }

    @Test
    @DisplayName("shouldExportCsvInEnglish_whenLanguageIsEn")
    void shouldExportCsvInEnglish_whenLanguageIsEn() {
        // Arrange
        String tenantId = TENANT_TEST;
        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 1, 31);
        String role = "USER";
        String owner = "john";
        String department = "sales";
        String language = "en-US";

        Map<String, Object> mockReport = createMockReport();
        when(reportService.overviewByTenant(tenantId, fromDate, toDate, role, owner, department))
                .thenReturn(mockReport);

        // Act
        String csv = exportService.exportOverviewCsvByTenant(tenantId, fromDate, toDate, role, owner, department, language);

        // Assert
        assertNotNull(csv);
        assertTrue(csv.contains("section,key,value")); // English header
        assertTrue(csv.contains("filter"));
        assertTrue(csv.contains("summary"));
        assertFalse(csv.contains("\u5206\u7EC4")); // No Chinese
    }

    @Test
    @DisplayName("shouldHandleNullDates_whenExportingCsv")
    void shouldHandleNullDates_whenExportingCsv() {
        // Arrange
        String tenantId = TENANT_TEST;
        LocalDate fromDate = null;
        LocalDate toDate = null;
        String role = "USER";
        String owner = "";
        String department = "";

        Map<String, Object> mockReport = createMockReport();
        when(reportService.overviewByTenant(tenantId, null, null, role, owner, department))
                .thenReturn(mockReport);

        // Act
        String csv = exportService.exportOverviewCsvByTenant(tenantId, fromDate, toDate, role, owner, department, "en");

        // Assert
        assertNotNull(csv);
        assertTrue(csv.contains("section,key,value"));
    }

    @Test
    @DisplayName("shouldEscapeSpecialCharactersInCsv_whenDataContainsCommas")
    void shouldEscapeSpecialCharactersInCsv_whenDataContainsCommas() {
        // Arrange
        String tenantId = "tenant-1";
        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 1, 31);
        String role = "ADMIN";
        String owner = "";
        String department = "";

        Map<String, Object> mockReport = createMockReportWithSpecialChars();
        when(reportService.overviewByTenant(tenantId, fromDate, toDate, role, owner, department))
                .thenReturn(mockReport);

        // Act
        String csv = exportService.exportOverviewCsvByTenant(tenantId, fromDate, toDate, role, owner, department, "en");

        // Assert
        assertNotNull(csv);
        // Values should be quoted
        assertTrue(csv.contains("\"")); // CSV values should be quoted
    }

    private Map<String, Object> createMockReport() {
        Map<String, Object> report = new HashMap<String, Object>();

        Map<String, Object> summary = new HashMap<String, Object>();
        summary.put("customers", 100);
        summary.put("revenue", 50000L);
        summary.put("opportunities", 25);
        summary.put("weightedAmount", 30000L);
        summary.put("winRate", 75.0);
        summary.put("taskDoneRate", 80.0);
        summary.put("followUps", 150);
        summary.put("quotes", 30);
        summary.put("orders", 20);
        summary.put("quoteApproveRate", 60.0);
        summary.put("quoteToOrderRate", 50.0);
        summary.put("orderCompleteRate", 70.0);
        summary.put("orderPaymentReceived", 40000L);
        summary.put("orderPaymentOutstanding", 10000L);
        summary.put("orderCollectionRate", 80.0);
        report.put("summary", summary);

        Map<String, Integer> customerByOwner = new HashMap<String, Integer>();
        customerByOwner.put("owner1", 50);
        customerByOwner.put("owner2", 50);
        report.put("customerByOwner", customerByOwner);

        Map<String, Long> revenueByStatus = new HashMap<String, Long>();
        revenueByStatus.put("ACTIVE", 40000L);
        revenueByStatus.put("PENDING", 10000L);
        report.put("revenueByStatus", revenueByStatus);

        Map<String, Integer> opportunityByStage = new HashMap<String, Integer>();
        opportunityByStage.put("PROPOSAL", 10);
        opportunityByStage.put("NEGOTIATION", 15);
        report.put("opportunityByStage", opportunityByStage);

        Map<String, Integer> taskStatus = new HashMap<String, Integer>();
        taskStatus.put("done", 80);
        taskStatus.put("pending", 20);
        report.put("taskStatus", taskStatus);

        Map<String, Integer> followUpByChannel = new HashMap<String, Integer>();
        followUpByChannel.put("PHONE", 50);
        followUpByChannel.put("EMAIL", 100);
        report.put("followUpByChannel", followUpByChannel);

        Map<String, Integer> quoteByStatus = new HashMap<String, Integer>();
        quoteByStatus.put("APPROVED", 15);
        quoteByStatus.put("PENDING", 15);
        report.put("quoteByStatus", quoteByStatus);

        Map<String, Integer> orderByStatus = new HashMap<String, Integer>();
        orderByStatus.put("COMPLETED", 14);
        orderByStatus.put("PENDING", 6);
        report.put("orderByStatus", orderByStatus);

        return report;
    }

    private Map<String, Object> createEmptyReport() {
        Map<String, Object> report = new HashMap<String, Object>();

        Map<String, Object> summary = new HashMap<String, Object>();
        summary.put("customers", 0);
        summary.put("revenue", 0L);
        summary.put("opportunities", 0);
        summary.put("weightedAmount", 0L);
        summary.put("winRate", 0.0);
        summary.put("taskDoneRate", 0.0);
        summary.put("followUps", 0);
        summary.put("quotes", 0);
        summary.put("orders", 0);
        summary.put("quoteApproveRate", 0.0);
        summary.put("quoteToOrderRate", 0.0);
        summary.put("orderCompleteRate", 0.0);
        summary.put("orderPaymentReceived", 0L);
        summary.put("orderPaymentOutstanding", 0L);
        summary.put("orderCollectionRate", 0.0);
        report.put("summary", summary);

        report.put("customerByOwner", new HashMap<String, Integer>());
        report.put("revenueByStatus", new HashMap<String, Long>());
        report.put("opportunityByStage", new HashMap<String, Integer>());
        report.put("taskStatus", new HashMap<String, Integer>());
        report.put("followUpByChannel", new HashMap<String, Integer>());
        report.put("quoteByStatus", new HashMap<String, Integer>());
        report.put("orderByStatus", new HashMap<String, Integer>());

        return report;
    }

    private Map<String, Object> createMockReportWithSpecialChars() {
        Map<String, Object> report = createMockReport();

        Map<String, Integer> customerByOwner = new HashMap<String, Integer>();
        customerByOwner.put("owner, with comma", 50);
        report.put("customerByOwner", customerByOwner);

        return report;
    }
}

