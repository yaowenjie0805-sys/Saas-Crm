package com.yao.crm.controller;

import com.yao.crm.dto.request.CreateFollowUpRequest;
import com.yao.crm.dto.request.UpdateFollowUpRequest;
import com.yao.crm.entity.Customer;
import com.yao.crm.entity.FollowUp;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.FollowUpRepository;
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

import java.util.Optional;

import static com.yao.crm.support.TestTenant.TENANT_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowUpControllerTest {

    @Mock
    private FollowUpRepository followUpRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ValueNormalizerService valueNormalizerService;
    @Mock
    private IdGenerator idGenerator;

    private MockHttpServletRequest request;

    private FollowUpController controller;

    @BeforeEach
    void setUp() {
        controller = new FollowUpController(followUpRepository, customerRepository, auditLogService, valueNormalizerService, new I18nService(), idGenerator);
        request = new MockHttpServletRequest();
        request.setAttribute("authRole", "MANAGER");
        request.setAttribute("authUsername", "manager");
        request.setAttribute("authTenantId", TENANT_TEST);
    }

    @Test
    void deleteFollowUpShouldDeleteWithinTenantAndReturnNoContent() {
        when(followUpRepository.deleteByIdAndTenantId("fu-1", TENANT_TEST)).thenReturn(1L);

        ResponseEntity<?> response = controller.deleteFollowUp(request, "fu-1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(followUpRepository, never()).findByIdAndTenantId(anyString(), anyString());
        verify(followUpRepository).deleteByIdAndTenantId("fu-1", TENANT_TEST);
        verify(auditLogService).record(eq("manager"), eq("MANAGER"), eq("DELETE"), eq("FOLLOW_UP"), eq("fu-1"), anyString(), eq(TENANT_TEST));
    }

    @Test
    void deleteFollowUpShouldTrimIdBeforeTenantScopedDelete() {
        when(followUpRepository.deleteByIdAndTenantId("fu-1", TENANT_TEST)).thenReturn(1L);

        ResponseEntity<?> response = controller.deleteFollowUp(request, "  fu-1  ");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(followUpRepository, never()).findByIdAndTenantId(anyString(), anyString());
        verify(followUpRepository).deleteByIdAndTenantId("fu-1", TENANT_TEST);
        verify(auditLogService).record(eq("manager"), eq("MANAGER"), eq("DELETE"), eq("FOLLOW_UP"), eq("fu-1"), anyString(), eq(TENANT_TEST));
    }

    @Test
    void deleteFollowUpShouldReturnBadRequestForBlankIdWithoutSideEffects() {
        ResponseEntity<?> response = controller.deleteFollowUp(request, "");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(followUpRepository, never()).findByIdAndTenantId(anyString(), anyString());
        verify(followUpRepository, never()).deleteByIdAndTenantId(anyString(), anyString());
        verify(auditLogService, never()).record(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void updateFollowUpShouldReturnBadRequestForBlankIdWithoutSideEffects() {
        ResponseEntity<?> response = controller.updateFollowUp(request, " ", new UpdateFollowUpRequest());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(followUpRepository, never()).findByIdAndTenantId(anyString(), anyString());
        verify(followUpRepository, never()).save(any(FollowUp.class));
        verify(auditLogService, never()).record(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void deleteFollowUpShouldReturnNotFoundWhenTenantScopedDeleteAffectsZeroRows() {
        when(followUpRepository.deleteByIdAndTenantId("fu-1", TENANT_TEST)).thenReturn(0L);

        ResponseEntity<?> response = controller.deleteFollowUp(request, "fu-1");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(followUpRepository, never()).findByIdAndTenantId(anyString(), anyString());
        verify(followUpRepository).deleteByIdAndTenantId("fu-1", TENANT_TEST);
        verify(auditLogService, never()).record(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void createFollowUpShouldReturnBadRequestWhenNextActionDateIsInvalid() {
        CreateFollowUpRequest payload = new CreateFollowUpRequest();
        payload.setCustomerId("  cust-1  ");
        payload.setSummary("follow up");
        payload.setNextActionDate("2026-02-30");

        Customer customer = new Customer();
        customer.setId("cust-1");
        customer.setTenantId(TENANT_TEST);
        customer.setOwner("manager");
        when(customerRepository.findByIdAndTenantId("cust-1", TENANT_TEST)).thenReturn(Optional.of(customer));

        ResponseEntity<?> response = controller.createFollowUp(request, payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(customerRepository).findByIdAndTenantId("cust-1", TENANT_TEST);
        verify(followUpRepository, never()).save(any(FollowUp.class));
        verify(auditLogService, never()).record(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void deleteFollowUpShouldReturnForbiddenForSalesOutsideScope() {
        request.setAttribute("authRole", "SALES");
        request.setAttribute("authUsername", "sales-1");
        request.setAttribute("authOwnerScope", "sales-scope");
        FollowUp followUp = new FollowUp();
        followUp.setId("fu-1");
        followUp.setTenantId(TENANT_TEST);
        followUp.setCustomerId("cust-1");
        Customer customer = new Customer();
        customer.setId("cust-1");
        customer.setTenantId(TENANT_TEST);
        customer.setOwner("another-owner");
        when(followUpRepository.findByIdAndTenantId("fu-1", TENANT_TEST)).thenReturn(Optional.of(followUp));
        when(customerRepository.findByIdAndTenantId("cust-1", TENANT_TEST)).thenReturn(Optional.of(customer));

        ResponseEntity<?> response = controller.deleteFollowUp(request, "fu-1");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(followUpRepository).findByIdAndTenantId("fu-1", TENANT_TEST);
        verify(customerRepository).findByIdAndTenantId("cust-1", TENANT_TEST);
        verify(followUpRepository, never()).deleteByIdAndTenantId("fu-1", TENANT_TEST);
        verify(auditLogService, never()).record(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }
}
