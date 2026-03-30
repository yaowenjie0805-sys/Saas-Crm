package com.yao.crm.controller;

import com.yao.crm.dto.request.UpdateContractRequest;
import com.yao.crm.repository.ContractRecordRepository;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import com.yao.crm.service.ValueNormalizerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractControllerTest {

    @Mock
    private ContractRecordRepository contractRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ValueNormalizerService valueNormalizerService;

    private MockHttpServletRequest request;

    private ContractController controller;

    @BeforeEach
    void setUp() {
        controller = new ContractController(contractRepository, customerRepository, auditLogService, valueNormalizerService, new I18nService());
        request = new MockHttpServletRequest();
        request.setAttribute("authRole", "MANAGER");
        request.setAttribute("authUsername", "manager");
        request.setAttribute("authTenantId", "tenant-1");
    }

    @Test
    void deleteContractShouldDeleteWithinTenantAndReturnNoContent() {
        when(contractRepository.deleteByIdAndTenantId("cr-1", "tenant-1")).thenReturn(1L);

        ResponseEntity<?> response = controller.deleteContract(request, "cr-1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(contractRepository, times(1)).deleteByIdAndTenantId("cr-1", "tenant-1");
        verify(auditLogService).record(eq("manager"), eq("MANAGER"), eq("DELETE"), eq("CONTRACT"), eq("cr-1"), anyString());
    }

    @Test
    void deleteContractShouldTrimIdBeforeTenantScopedDelete() {
        when(contractRepository.deleteByIdAndTenantId("cr-1", "tenant-1")).thenReturn(1L);

        ResponseEntity<?> response = controller.deleteContract(request, "  cr-1  ");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(contractRepository).deleteByIdAndTenantId("cr-1", "tenant-1");
        verify(auditLogService).record(eq("manager"), eq("MANAGER"), eq("DELETE"), eq("CONTRACT"), eq("cr-1"), anyString());
    }

    @Test
    void deleteContractShouldReturnNotFoundWhenTenantScopedDeleteAffectsZeroRows() {
        when(contractRepository.deleteByIdAndTenantId("cr-1", "tenant-1")).thenReturn(0L);

        ResponseEntity<?> response = controller.deleteContract(request, "cr-1");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(contractRepository, times(1)).deleteByIdAndTenantId("cr-1", "tenant-1");
        verify(auditLogService, never()).record(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void deleteContractShouldReturnBadRequestWhenIdIsBlank() {
        ResponseEntity<?> response = controller.deleteContract(request, "   ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(contractRepository, never()).deleteByIdAndTenantId(anyString(), anyString());
        verify(auditLogService, never()).record(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void updateContractShouldReturnBadRequestWhenIdIsBlank() {
        ResponseEntity<?> response = controller.updateContract(request, "   ", new UpdateContractRequest());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(contractRepository, never()).findByIdAndTenantId(anyString(), anyString());
        verify(contractRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(auditLogService, never()).record(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void updateContractShouldTrimIdBeforeTenantScopedLookup() {
        when(contractRepository.findByIdAndTenantId("cr-1", "tenant-1")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.updateContract(request, "  cr-1  ", new UpdateContractRequest());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(contractRepository).findByIdAndTenantId("cr-1", "tenant-1");
        verify(contractRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(auditLogService, never()).record(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }
}
