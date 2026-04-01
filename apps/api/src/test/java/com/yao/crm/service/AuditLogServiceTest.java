package com.yao.crm.service;

import static com.yao.crm.support.TestTenant.TENANT_TEST;

import com.yao.crm.entity.AuditLog;
import com.yao.crm.repository.AuditLogRepository;
import com.yao.crm.util.IdGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {
    private static final String TENANT_CTX = TENANT_TEST + "_ctx";
    private static final String TENANT_HEADER = TENANT_TEST + "_header";
    private static final String TENANT_EXPLICIT = TENANT_TEST + "_explicit";

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private IdGenerator idGenerator;

    private AuditLogService service;

    @BeforeEach
    void setUp() {
        service = new AuditLogService(auditLogRepository, idGenerator);
        RequestContextHolder.resetRequestAttributes();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void recordShouldFailFastWhenTenantIsMissing() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.record("alice", "ADMIN", "CREATE", "CUSTOMER", "cus-1", "created")
        );

        assertEquals("tenant_id_required", error.getMessage());
        verifyNoInteractions(idGenerator, auditLogRepository);
    }

    @Test
    void recordWithSevenArgsShouldFailFastWhenTenantIsBlank() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.record("alice", "ADMIN", "CREATE", "CUSTOMER", "cus-1", "created", "   ")
        );

        assertEquals("tenant_id_required", error.getMessage());
        verifyNoInteractions(idGenerator, auditLogRepository);
    }

    @Test
    void recordShouldUseTenantFromRequestContextWhenTenantArgumentMissing() {
        bindRequestTenant(TENANT_CTX, null);
        when(idGenerator.generate("log")).thenReturn("log-1");

        service.record("alice", "ADMIN", "CREATE", "CUSTOMER", "cus-1", "created");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals("log-1", saved.getId());
        assertEquals(TENANT_CTX, saved.getTenantId());
    }

    @Test
    void recordShouldUseTenantFromHeaderWhenAuthTenantMissing() {
        bindRequestTenant(null, TENANT_HEADER);
        when(idGenerator.generate("log")).thenReturn("log-header");

        service.record("alice", "ADMIN", "CREATE", "CUSTOMER", "cus-1", "created");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(TENANT_HEADER, captor.getValue().getTenantId());
    }

    @Test
    void recordShouldPreferExplicitTenantOverRequestContext() {
        bindRequestTenant(TENANT_CTX, null);
        when(idGenerator.generate("log")).thenReturn("log-2");

        service.record("alice", "ADMIN", "CREATE", "CUSTOMER", "cus-1", "created", "  " + TENANT_EXPLICIT + "  ");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(TENANT_EXPLICIT, captor.getValue().getTenantId());
    }

    @Test
    void latestByTenantShouldFailFastWhenTenantMissing() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.latestByTenant("   ")
        );

        assertEquals("tenant_id_required", error.getMessage());
        verify(auditLogRepository, never()).findTop100ByTenantIdOrderByCreatedAtDesc(eq(TENANT_TEST));
    }

    @Test
    void latestByTenantShouldFailFastWhenTenantIsNullAndNoRequestContext() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.latestByTenant(null)
        );

        assertEquals("tenant_id_required", error.getMessage());
        verify(auditLogRepository, never()).findTop100ByTenantIdOrderByCreatedAtDesc(eq(TENANT_TEST));
    }

    @Test
    void latestByTenantShouldUseTenantFromRequestContextWhenInputBlank() {
        bindRequestTenant(null, TENANT_HEADER);
        List<AuditLog> expected = Collections.emptyList();
        when(auditLogRepository.findTop100ByTenantIdOrderByCreatedAtDesc(TENANT_HEADER))
                .thenReturn(expected);

        List<AuditLog> result = service.latestByTenant(" ");

        assertSame(expected, result);
        verify(auditLogRepository).findTop100ByTenantIdOrderByCreatedAtDesc(TENANT_HEADER);
    }

    private void bindRequestTenant(String authTenantId, String headerTenantId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authTenantId != null) {
            request.setAttribute("authTenantId", authTenantId);
        }
        if (headerTenantId != null) {
            request.addHeader("X-Tenant-Id", headerTenantId);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
