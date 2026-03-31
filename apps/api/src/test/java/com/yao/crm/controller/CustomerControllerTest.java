package com.yao.crm.controller;

import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.dto.request.UpdateCustomerRequest;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import com.yao.crm.service.ValueNormalizerService;
import com.yao.crm.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ValueNormalizerService valueNormalizerService;
    @Mock
    private IdGenerator idGenerator;

    private MockHttpServletRequest request;

    private CustomerController controller;

    @BeforeEach
    void setUp() {
        controller = new CustomerController(customerRepository, auditLogService, valueNormalizerService, new I18nService(), idGenerator);
        request = new MockHttpServletRequest();
        request.setAttribute("authRole", "MANAGER");
        request.setAttribute("authUsername", "manager");
        request.setAttribute("authTenantId", "tenant-1");
    }

    @Test
    void deleteCustomerShouldDeleteWithinTenantAndReturnNoContent() {
        when(customerRepository.deleteByIdAndTenantId("cust-1", "tenant-1")).thenReturn(1L);

        ResponseEntity<?> response = controller.deleteCustomer(request, "cust-1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(customerRepository).deleteByIdAndTenantId("cust-1", "tenant-1");
        verify(customerRepository, never()).findByIdAndTenantId(anyString(), anyString());
        verifyNoMoreInteractions(customerRepository);
        verify(auditLogService).record(eq("manager"), eq("MANAGER"), eq("DELETE"), eq("CUSTOMER"), eq("cust-1"), anyString());
    }

    @Test
    void deleteCustomerShouldTrimIdBeforeTenantScopedDelete() {
        when(customerRepository.deleteByIdAndTenantId("cust-1", "tenant-1")).thenReturn(1L);

        ResponseEntity<?> response = controller.deleteCustomer(request, "  cust-1  ");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(customerRepository).deleteByIdAndTenantId("cust-1", "tenant-1");
        verify(customerRepository, never()).findByIdAndTenantId(anyString(), anyString());
        verifyNoMoreInteractions(customerRepository);
        verify(auditLogService).record(eq("manager"), eq("MANAGER"), eq("DELETE"), eq("CUSTOMER"), eq("cust-1"), anyString());
    }

    @Test
    void deleteCustomerShouldReturnNotFoundWhenTenantScopedDeleteAffectsZeroRows() {
        when(customerRepository.deleteByIdAndTenantId("cust-1", "tenant-1")).thenReturn(0L);

        ResponseEntity<?> response = controller.deleteCustomer(request, "cust-1");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(customerRepository).deleteByIdAndTenantId("cust-1", "tenant-1");
        verify(customerRepository, never()).findByIdAndTenantId(anyString(), anyString());
        verifyNoMoreInteractions(customerRepository);
        verify(auditLogService, never()).record(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void deleteCustomerShouldReturnBadRequestForBlankIdWithoutTouchingRepository() {
        ResponseEntity<?> response = controller.deleteCustomer(request, "   ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(customerRepository);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void updateCustomerShouldReturnBadRequestForBlankIdWithoutTouchingRepository() {
        ResponseEntity<?> response = controller.updateCustomer(request, "   ", new UpdateCustomerRequest());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(customerRepository);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void deleteCustomerShouldRejectUnauthorizedRolesWithoutTouchingRepository() {
        request.setAttribute("authRole", "SALES");

        ResponseEntity<?> response = controller.deleteCustomer(request, "cust-1");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(customerRepository);
        verifyNoInteractions(auditLogService);
    }
}
