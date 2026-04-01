package com.yao.crm.controller;

import com.yao.crm.dto.request.ChartTemplateCreateRequest;
import com.yao.crm.dto.request.ChartTemplateUpdateRequest;
import com.yao.crm.entity.ChartTemplate;
import com.yao.crm.service.ChartService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static com.yao.crm.support.TestTenant.TENANT_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;

class ChartControllerTest {

    @Test
    void deleteTemplateShouldReturnNoContentWithoutBody() {
        ChartController controller = new ChartController(mock(ChartService.class));

        ResponseEntity<?> response = controller.deleteTemplate(TENANT_TEST, "template-1");

        assertEquals(204, response.getStatusCodeValue());
        assertNull(response.getBody());
    }

    @Test
    void getChartDataShouldRejectBlankTenantWithBadRequest() {
        ChartService chartService = mock(ChartService.class);
        ChartController controller = new ChartController(chartService);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                controller.getChartData("   ", "CUSTOMERS", null, null, null, null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verifyNoInteractions(chartService);
    }

    @Test
    void deleteTemplateShouldRejectBlankTenantWithBadRequest() {
        ChartController controller = new ChartController(mock(ChartService.class));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                controller.deleteTemplate("   ", "template-1"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void deleteTemplateShouldRejectBlankIdWithBadRequest() {
        ChartController controller = new ChartController(mock(ChartService.class));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                controller.deleteTemplate(TENANT_TEST, "   "));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void getTemplateShouldTrimIdInResponse() {
        ChartController controller = new ChartController(mock(ChartService.class));

        ResponseEntity<?> response = controller.getTemplate(TENANT_TEST, "  template-1  ");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("template-1", ((java.util.Map<?, ?>) response.getBody()).get("id"));
    }

    @Test
    void updateTemplateShouldRejectBlankIdWithBadRequest() {
        ChartController controller = new ChartController(mock(ChartService.class));
        ChartTemplateUpdateRequest request = new ChartTemplateUpdateRequest();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                controller.updateTemplate(TENANT_TEST, "   ", request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void getChartDataShouldTrimTenantBeforeCallingService() {
        ChartService chartService = mock(ChartService.class);
        ChartController controller = new ChartController(chartService);

        controller.getChartData("  tenant-1  ", "CUSTOMERS", null, null, null, null);

        verify(chartService).getChartData(eq(TENANT_TEST), eq("CUSTOMERS"), anyMap());
    }

    @Test
    void createTemplateShouldTrimTenantBeforePassingTemplateToService() {
        ChartService chartService = mock(ChartService.class);
        ChartController controller = new ChartController(chartService);
        ChartTemplateCreateRequest request = new ChartTemplateCreateRequest();
        request.setName("Pipeline");
        request.setChartType("BAR");
        request.setDatasetType("CUSTOMERS");

        ResponseEntity<ChartTemplate> response = controller.createTemplate("  tenant-1  ", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(TENANT_TEST, response.getBody().getTenantId());

        ArgumentCaptor<ChartTemplate> captor = ArgumentCaptor.forClass(ChartTemplate.class);
        verify(chartService).createTemplate(captor.capture());
        assertEquals(TENANT_TEST, captor.getValue().getTenantId());
    }

    @Test
    void getTemplatesWithSystemShouldNotMutateOriginalSystemList() {
        ChartService chartService = mock(ChartService.class);
        ChartController controller = new ChartController(chartService);

        ChartTemplate systemTemplate = new ChartTemplate();
        systemTemplate.setId("system-1");
        ChartTemplate tenantTemplate = new ChartTemplate();
        tenantTemplate.setId(TENANT_TEST);

        List<ChartTemplate> systemTemplates = new ArrayList<>();
        systemTemplates.add(systemTemplate);
        List<ChartTemplate> tenantTemplates = new ArrayList<>();
        tenantTemplates.add(tenantTemplate);

        when(chartService.getSystemTemplates(TENANT_TEST)).thenReturn(systemTemplates);
        when(chartService.getTemplates(TENANT_TEST, null)).thenReturn(tenantTemplates);

        ResponseEntity<List<ChartTemplate>> response = controller.getTemplates(" tenant-1 ", null, true);

        verify(chartService).getSystemTemplates(TENANT_TEST);
        verify(chartService).getTemplates(TENANT_TEST, null);
        assertEquals(1, systemTemplates.size());
        assertSame(systemTemplate, systemTemplates.get(0));
        assertEquals(2, response.getBody().size());
        assertSame(systemTemplate, response.getBody().get(0));
        assertSame(tenantTemplate, response.getBody().get(1));
        assertTrue(response.getBody() != systemTemplates);
    }
}

