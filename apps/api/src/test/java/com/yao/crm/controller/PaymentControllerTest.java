package com.yao.crm.controller;

import com.yao.crm.dto.request.UpdatePaymentRequest;
import com.yao.crm.repository.ContractRecordRepository;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.OrderRecordRepository;
import com.yao.crm.repository.PaymentRecordRepository;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentRecordRepository paymentRepository;

    @Mock
    private ContractRecordRepository contractRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OrderRecordRepository orderRecordRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ValueNormalizerService valueNormalizerService;
    @Mock
    private IdGenerator idGenerator;

    private MockHttpServletRequest request;

    private PaymentController controller;

    @BeforeEach
    void setUp() {
        controller = new PaymentController(
                paymentRepository,
                contractRepository,
                customerRepository,
                orderRecordRepository,
                auditLogService,
                valueNormalizerService,
                new I18nService(),
                idGenerator
        );
        request = new MockHttpServletRequest();
        request.setAttribute("authRole", "MANAGER");
        request.setAttribute("authUsername", "manager");
        request.setAttribute("authTenantId", TENANT_TEST);
    }

    @Test
    void deletePaymentShouldDeleteWithinTenantAndReturnNoContent() {
        when(paymentRepository.deleteByIdAndTenantId("pay-1", TENANT_TEST)).thenReturn(1L);

        ResponseEntity<?> response = controller.deletePayment(request, "pay-1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(paymentRepository, times(1)).deleteByIdAndTenantId("pay-1", TENANT_TEST);
        verify(auditLogService).record(eq("manager"), eq("MANAGER"), eq("DELETE"), eq("PAYMENT"), eq("pay-1"), anyString(), eq(TENANT_TEST));
    }

    @Test
    void deletePaymentShouldTrimIdBeforeTenantScopedDelete() {
        when(paymentRepository.deleteByIdAndTenantId("pay-1", TENANT_TEST)).thenReturn(1L);

        ResponseEntity<?> response = controller.deletePayment(request, "  pay-1  ");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(paymentRepository).deleteByIdAndTenantId("pay-1", TENANT_TEST);
        verify(auditLogService).record(eq("manager"), eq("MANAGER"), eq("DELETE"), eq("PAYMENT"), eq("pay-1"), anyString(), eq(TENANT_TEST));
    }

    @Test
    void deletePaymentShouldReturnNotFoundWhenTenantScopedDeleteAffectsZeroRows() {
        when(paymentRepository.deleteByIdAndTenantId("pay-1", TENANT_TEST)).thenReturn(0L);

        ResponseEntity<?> response = controller.deletePayment(request, "pay-1");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(paymentRepository, times(1)).deleteByIdAndTenantId("pay-1", TENANT_TEST);
        verify(auditLogService, never()).record(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void deletePaymentShouldReturnBadRequestWhenIdIsBlank() {
        ResponseEntity<?> response = controller.deletePayment(request, " ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(paymentRepository, never()).deleteByIdAndTenantId(anyString(), anyString());
        verify(auditLogService, never()).record(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void updatePaymentShouldReturnBadRequestWhenIdIsBlank() {
        ResponseEntity<?> response = controller.updatePayment(request, "   ", new UpdatePaymentRequest());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(paymentRepository, never()).findByIdAndTenantId(anyString(), anyString());
        verify(paymentRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(auditLogService, never()).record(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void updatePaymentShouldTrimIdBeforeTenantScopedLookup() {
        when(paymentRepository.findByIdAndTenantId("pay-1", TENANT_TEST)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.updatePayment(request, "  pay-1  ", new UpdatePaymentRequest());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(paymentRepository).findByIdAndTenantId("pay-1", TENANT_TEST);
        verify(paymentRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(auditLogService, never()).record(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }
}
