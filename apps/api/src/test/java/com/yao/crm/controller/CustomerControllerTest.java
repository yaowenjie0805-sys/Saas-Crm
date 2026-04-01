package com.yao.crm.controller;

import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.dto.request.CreateCustomerRequest;
import com.yao.crm.dto.request.UpdateCustomerRequest;
import com.yao.crm.entity.Customer;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import com.yao.crm.service.ValueNormalizerService;
import com.yao.crm.util.IdGenerator;
import java.util.Collections;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static com.yao.crm.support.TestTenant.TENANT_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
        request.setAttribute("authTenantId", TENANT_TEST);
    }

    @Test
    void deleteCustomerShouldDeleteWithinTenantAndReturnNoContent() {
        when(customerRepository.deleteByIdAndTenantId("cust-1", TENANT_TEST)).thenReturn(1L);

        ResponseEntity<?> response = controller.deleteCustomer(request, "cust-1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(customerRepository).deleteByIdAndTenantId("cust-1", TENANT_TEST);
        verify(customerRepository, never()).findByIdAndTenantId(anyString(), anyString());
        verifyNoMoreInteractions(customerRepository);
        verify(auditLogService).record(eq("manager"), eq("MANAGER"), eq("DELETE"), eq("CUSTOMER"), eq("cust-1"), anyString());
    }

    @Test
    void deleteCustomerShouldTrimIdBeforeTenantScopedDelete() {
        when(customerRepository.deleteByIdAndTenantId("cust-1", TENANT_TEST)).thenReturn(1L);

        ResponseEntity<?> response = controller.deleteCustomer(request, "  cust-1  ");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(customerRepository).deleteByIdAndTenantId("cust-1", TENANT_TEST);
        verify(customerRepository, never()).findByIdAndTenantId(anyString(), anyString());
        verifyNoMoreInteractions(customerRepository);
        verify(auditLogService).record(eq("manager"), eq("MANAGER"), eq("DELETE"), eq("CUSTOMER"), eq("cust-1"), anyString());
    }

    @Test
    void deleteCustomerShouldReturnNotFoundWhenTenantScopedDeleteAffectsZeroRows() {
        when(customerRepository.deleteByIdAndTenantId("cust-1", TENANT_TEST)).thenReturn(0L);

        ResponseEntity<?> response = controller.deleteCustomer(request, "cust-1");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(customerRepository).deleteByIdAndTenantId("cust-1", TENANT_TEST);
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

    @Test
    void customersShouldReturnPageOfCustomers() {
        Customer customer = new Customer();
        customer.setId("cust-1");
        customer.setName("Test Customer");
        customer.setTenantId(TENANT_TEST);
        Page<Customer> page = new PageImpl<>(Collections.singletonList(customer));

        when(customerRepository.findByTenantId(eq(TENANT_TEST), any(Pageable.class))).thenReturn(page);

        ResponseEntity<?> response = controller.customers(request, 1, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(customerRepository).findByTenantId(eq(TENANT_TEST), any(Pageable.class));
    }

    @Test
    void searchCustomersShouldReturnPageOfCustomers() {
        Customer customer = new Customer();
        customer.setId("cust-1");
        customer.setName("Test Customer");
        customer.setTenantId(TENANT_TEST);
        Page<Customer> page = new PageImpl<>(Collections.singletonList(customer));

        when(customerRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        ResponseEntity<?> response = controller.searchCustomers(request, "test", "Active", 1, 10, "name", "asc");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(customerRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void searchCustomersShouldRejectUnauthorizedRoles() {
        request.setAttribute("authRole", "USER");

        ResponseEntity<?> response = controller.searchCustomers(request, "test", "Active", 1, 10, "name", "asc");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(customerRepository);
    }

    @Test
    void createCustomerShouldCreateAndReturnCreated() {
        CreateCustomerRequest payload = new CreateCustomerRequest();
        payload.setName("New Customer");
        payload.setOwner("sales");
        payload.setStatus("Active");
        payload.setTag("New");
        payload.setValue(1000L);

        Customer customer = new Customer();
        customer.setId("c_123");
        customer.setTenantId(TENANT_TEST);
        customer.setName("New Customer");
        customer.setOwner("sales");
        customer.setStatus("active");
        customer.setTag("New");
        customer.setValue(1000L);

        when(valueNormalizerService.isValidCustomerStatus("Active")).thenReturn(true);
        when(valueNormalizerService.normalizeCustomerStatus("Active")).thenReturn("active");
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        ResponseEntity<?> response = controller.createCustomer(request, payload);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(customerRepository).save(any(Customer.class));
        verify(auditLogService).record(eq("manager"), eq("MANAGER"), eq("CREATE"), eq("CUSTOMER"), eq("c_123"), eq("New Customer"));
    }

    @Test
    void createCustomerShouldRejectUnauthorizedRoles() {
        request.setAttribute("authRole", "USER");
        CreateCustomerRequest payload = new CreateCustomerRequest();

        ResponseEntity<?> response = controller.createCustomer(request, payload);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(customerRepository);
    }

    @Test
    void createCustomerShouldRejectInvalidStatus() {
        CreateCustomerRequest payload = new CreateCustomerRequest();
        payload.setName("New Customer");
        payload.setStatus("Invalid");

        when(valueNormalizerService.isValidCustomerStatus("Invalid")).thenReturn(false);

        ResponseEntity<?> response = controller.createCustomer(request, payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(customerRepository);
    }

    @Test
    void updateCustomerShouldUpdateAndReturnOk() {
        Customer existingCustomer = new Customer();
        existingCustomer.setId("cust-1");
        existingCustomer.setName("Old Name");
        existingCustomer.setTenantId(TENANT_TEST);
        existingCustomer.setOwner("sales");

        UpdateCustomerRequest patch = new UpdateCustomerRequest();
        patch.setName("New Name");
        patch.setStatus("Active");

        Customer updatedCustomer = new Customer();
        updatedCustomer.setId("cust-1");
        updatedCustomer.setName("New Name");
        updatedCustomer.setTenantId(TENANT_TEST);
        updatedCustomer.setOwner("sales");
        updatedCustomer.setStatus("active");

        when(customerRepository.findByIdAndTenantId("cust-1", TENANT_TEST)).thenReturn(java.util.Optional.of(existingCustomer));
        when(valueNormalizerService.isValidCustomerStatus("Active")).thenReturn(true);
        when(valueNormalizerService.normalizeCustomerStatus("Active")).thenReturn("active");
        when(customerRepository.save(any(Customer.class))).thenReturn(updatedCustomer);

        ResponseEntity<?> response = controller.updateCustomer(request, "cust-1", patch);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(customerRepository).findByIdAndTenantId("cust-1", TENANT_TEST);
        verify(customerRepository).save(any(Customer.class));
        verify(auditLogService).record(eq("manager"), eq("MANAGER"), eq("UPDATE"), eq("CUSTOMER"), eq("cust-1"), eq("Updated customer fields"));
    }

    @Test
    void updateCustomerShouldReturnNotFoundForNonExistentCustomer() {
        UpdateCustomerRequest patch = new UpdateCustomerRequest();

        when(customerRepository.findByIdAndTenantId("cust-1", TENANT_TEST)).thenReturn(java.util.Optional.empty());

        ResponseEntity<?> response = controller.updateCustomer(request, "cust-1", patch);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(customerRepository).findByIdAndTenantId("cust-1", TENANT_TEST);
        verifyNoMoreInteractions(customerRepository);
    }

    @Test
    void updateCustomerShouldRejectUnauthorizedRoles() {
        request.setAttribute("authRole", "USER");
        UpdateCustomerRequest patch = new UpdateCustomerRequest();

        ResponseEntity<?> response = controller.updateCustomer(request, "cust-1", patch);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(customerRepository);
    }
}


