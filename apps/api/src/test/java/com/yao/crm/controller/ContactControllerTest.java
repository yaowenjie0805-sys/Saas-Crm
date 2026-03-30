package com.yao.crm.controller;

import com.yao.crm.dto.request.UpdateContactRequest;
import com.yao.crm.entity.Contact;
import com.yao.crm.entity.Customer;
import com.yao.crm.repository.ContactRepository;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactControllerTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AuditLogService auditLogService;

    private MockHttpServletRequest request;

    private ContactController controller;

    @BeforeEach
    void setUp() {
        controller = new ContactController(contactRepository, customerRepository, auditLogService, new I18nService());
        request = new MockHttpServletRequest();
        request.setAttribute("authRole", "MANAGER");
        request.setAttribute("authUsername", "manager");
        request.setAttribute("authTenantId", "tenant-1");
    }

    @Test
    void deleteContactShouldDeleteWithinTenantAndReturnNoContent() {
        when(contactRepository.deleteByIdAndTenantId("ct-1", "tenant-1")).thenReturn(1L);

        ResponseEntity<?> response = controller.deleteContact(request, "ct-1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(contactRepository).deleteByIdAndTenantId("ct-1", "tenant-1");
        verify(contactRepository, never()).findByIdAndTenantId(anyString(), anyString());
        verify(auditLogService).record(eq("manager"), eq("MANAGER"), eq("DELETE"), eq("CONTACT"), eq("ct-1"), anyString());
    }

    @Test
    void deleteContactShouldTrimIdBeforeTenantScopedDelete() {
        when(contactRepository.deleteByIdAndTenantId("ct-1", "tenant-1")).thenReturn(1L);

        ResponseEntity<?> response = controller.deleteContact(request, "  ct-1  ");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(contactRepository).deleteByIdAndTenantId("ct-1", "tenant-1");
        verify(contactRepository, never()).findByIdAndTenantId(anyString(), anyString());
        verify(auditLogService).record(eq("manager"), eq("MANAGER"), eq("DELETE"), eq("CONTACT"), eq("ct-1"), anyString());
    }

    @Test
    void deleteContactShouldReturnBadRequestForBlankIdWithoutSideEffects() {
        ResponseEntity<?> response = controller.deleteContact(request, "   ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(contactRepository, never()).findByIdAndTenantId(anyString(), anyString());
        verify(contactRepository, never()).deleteByIdAndTenantId(anyString(), anyString());
        verify(auditLogService, never()).record(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void updateContactShouldReturnBadRequestForBlankIdWithoutSideEffects() {
        UpdateContactRequest patch = new UpdateContactRequest();

        ResponseEntity<?> response = controller.updateContact(request, "   ", patch);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(contactRepository, never()).findByIdAndTenantId(anyString(), anyString());
        verify(contactRepository, never()).save(org.mockito.ArgumentMatchers.any(Contact.class));
        verify(customerRepository, never()).findByIdAndTenantId(anyString(), anyString());
        verify(auditLogService, never()).record(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void updateContactShouldTrimIdAndCustomerIdBeforeRepositoryCalls() {
        Contact existing = new Contact();
        existing.setId("ct-1");
        existing.setTenantId("tenant-1");
        existing.setOwner("manager");
        Customer customer = new Customer();
        customer.setId("cust-2");
        customer.setOwner("manager");
        when(contactRepository.findByIdAndTenantId("ct-1", "tenant-1")).thenReturn(Optional.of(existing));
        when(customerRepository.findByIdAndTenantId("cust-2", "tenant-1")).thenReturn(Optional.of(customer));
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UpdateContactRequest patch = new UpdateContactRequest();
        patch.setCustomerId("  cust-2  ");

        ResponseEntity<?> response = controller.updateContact(request, "  ct-1  ", patch);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(contactRepository).findByIdAndTenantId("ct-1", "tenant-1");
        verify(customerRepository).findByIdAndTenantId("cust-2", "tenant-1");
        ArgumentCaptor<Contact> savedContact = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).save(savedContact.capture());
        assertEquals("cust-2", savedContact.getValue().getCustomerId());
        verify(auditLogService).record(eq("manager"), eq("MANAGER"), eq("UPDATE"), eq("CONTACT"), eq("ct-1"), anyString());
    }

    @Test
    void updateContactShouldReturnBadRequestForBlankPatchedCustomerId() {
        Contact existing = new Contact();
        existing.setId("ct-1");
        existing.setTenantId("tenant-1");
        existing.setOwner("manager");
        when(contactRepository.findByIdAndTenantId("ct-1", "tenant-1")).thenReturn(Optional.of(existing));
        UpdateContactRequest patch = new UpdateContactRequest();
        patch.setCustomerId("   ");

        ResponseEntity<?> response = controller.updateContact(request, "  ct-1  ", patch);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(contactRepository).findByIdAndTenantId("ct-1", "tenant-1");
        verify(customerRepository, never()).findByIdAndTenantId(anyString(), anyString());
        verify(contactRepository, never()).save(any(Contact.class));
        verify(auditLogService, never()).record(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void deleteContactShouldReturnNotFoundWhenTenantScopedDeleteAffectsZeroRows() {
        when(contactRepository.deleteByIdAndTenantId("ct-1", "tenant-1")).thenReturn(0L);

        ResponseEntity<?> response = controller.deleteContact(request, "ct-1");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(contactRepository).deleteByIdAndTenantId("ct-1", "tenant-1");
        verify(contactRepository, never()).findByIdAndTenantId(anyString(), anyString());
        verify(auditLogService, never()).record(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void deleteContactShouldReturnForbiddenForSalesOutsideScope() {
        request.setAttribute("authRole", "SALES");
        request.setAttribute("authUsername", "sales-1");
        request.setAttribute("authOwnerScope", "sales-scope");
        Contact contact = new Contact();
        contact.setId("ct-1");
        contact.setTenantId("tenant-1");
        contact.setOwner("another-owner");
        when(contactRepository.findByIdAndTenantId("ct-1", "tenant-1")).thenReturn(Optional.of(contact));

        ResponseEntity<?> response = controller.deleteContact(request, "ct-1");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(contactRepository).findByIdAndTenantId("ct-1", "tenant-1");
        verify(contactRepository, never()).deleteByIdAndTenantId("ct-1", "tenant-1");
        verify(auditLogService, never()).record(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }
}
