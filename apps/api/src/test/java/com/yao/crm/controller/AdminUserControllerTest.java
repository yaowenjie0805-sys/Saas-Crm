package com.yao.crm.controller;

import com.yao.crm.dto.request.AdminUpdateUserRequest;
import com.yao.crm.entity.UserAccount;
import com.yao.crm.repository.UserAccountRepository;
import com.yao.crm.security.LoginRiskService;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.yao.crm.support.TestTenant.TENANT_TEST;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AdminUserController ?
 * 
 * ? * 1. listUsers - ? * 2. updateUser - nerScopeled? * 3. unlockUser - ?
 * 
 * ?
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AdminUserControllerTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private LoginRiskService loginRiskService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private I18nService i18nService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AdminUserController adminUserController;

    @BeforeEach
    void setUp() {
        // mock I18nService fallback
        when(i18nService.msg(any(HttpServletRequest.class), anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(1);
            return key;
        });
        when(request.getAttribute("authTenantId")).thenReturn(TENANT_TEST);
    }

    // ==================== listUsers ?====================

    /**
     * ?listUsers - ?     * ?ADMIN ?
     */
    @Test
    void testListUsers_Forbidden() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("SALES");

        // Act
        ResponseEntity<?> response = adminUserController.listUsers(request);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCodeValue());
        verify(userAccountRepository, never()).findAll();
        verify(userAccountRepository, never()).findAllByTenantIdOrderByUsernameAsc(anyString());
    }

    /**
     * ?listUsers - ?     */
    @Test
    void testListUsers_Success_EmptyList() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(userAccountRepository.findAllByTenantIdOrderByUsernameAsc(TENANT_TEST)).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<?> response = adminUserController.listUsers(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertTrue(items.isEmpty());
        verify(userAccountRepository).findAllByTenantIdOrderByUsernameAsc(TENANT_TEST);
    }

    /**
     * ?listUsers - ?     */
    @Test
    void testListUsers_Success_WithUsers() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");

        UserAccount user1 = createUser("zack", "Zack", "ADMIN", true);
        UserAccount user2 = createUser("alice", "Alice", "SALES", true);
        UserAccount user3 = createUser("bob", "Bob", "MANAGER", false);

        when(userAccountRepository.findAllByTenantIdOrderByUsernameAsc(TENANT_TEST)).thenReturn(Arrays.asList(user2, user3, user1));
        when(loginRiskService.isUserLocked(anyString(), anyString())).thenReturn(false);
        when(loginRiskService.remainingUserSeconds(anyString(), anyString())).thenReturn(0L);

        // Act
        ResponseEntity<?> response = adminUserController.listUsers(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertEquals(3, items.size());
        // ?
        assertEquals("alice", items.get(0).get("username"));
        assertEquals("bob", items.get(1).get("username"));
        assertEquals("zack", items.get(2).get("username"));
    }

    /**
     * ?listUsers - ?
     */
    @Test
    void testListUsers_Success_WithLockedUser() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");

        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        when(userAccountRepository.findAllByTenantIdOrderByUsernameAsc(TENANT_TEST)).thenReturn(Arrays.asList(user));
        when(loginRiskService.isUserLocked(TENANT_TEST, "testuser")).thenReturn(true);
        when(loginRiskService.remainingUserSeconds(TENANT_TEST, "testuser")).thenReturn(300L);

        // Act
        ResponseEntity<?> response = adminUserController.listUsers(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertEquals(1, items.size());
        assertTrue((Boolean) items.get(0).get("locked"));
        assertEquals(300L, items.get(0).get("lockRemainingSeconds"));
    }

    // ==================== updateUser ?====================

    /**
     * ?updateUser - ?     */
    @Test
    void testUpdateUser_Forbidden() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("SALES");
        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCodeValue());
        verify(userAccountRepository, never()).findByUsernameAndTenantId(anyString(), anyString());
    }

    /**
     * ?updateUser - ?     */
    @Test
    void testUpdateUser_UserNotFound() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(userAccountRepository.findByUsernameAndTenantId("nonexistent", TENANT_TEST)).thenReturn(Optional.empty());
        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setRole("ADMIN");

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "nonexistent", payload);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCodeValue());
    }

    /**
     * ?updateUser - ?
     */
    @Test
    void testUpdateUser_InvalidRole() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        when(userAccountRepository.findByUsernameAndTenantId("testuser", TENANT_TEST)).thenReturn(Optional.of(user));

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setRole("INVALID_ROLE");

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    /**
     * ?updateUser - ?ADMIN
     */
    @Test
    void testUpdateUser_Success_UpdateRoleToAdmin() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        user.setOwnerScope("testuser");
        when(userAccountRepository.findByUsernameAndTenantId("testuser", TENANT_TEST)).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loginRiskService.isUserLocked(TENANT_TEST, "testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds(TENANT_TEST, "testuser")).thenReturn(0L);

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setRole("ADMIN");

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userAccountRepository).save(any(UserAccount.class));
        verify(auditLogService).record(eq("admin"), eq("ADMIN"), eq("UPDATE"), eq("USER"), eq("testuser"), anyString(), eq(TENANT_TEST));
    }

    /**
     * ?updateUser - ?SALES?ownerScope?     */
    @Test
    void testUpdateUser_Success_UpdateRoleToSales() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "ADMIN", true);
        user.setOwnerScope("");
        when(userAccountRepository.findByUsernameAndTenantId("testuser", TENANT_TEST)).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loginRiskService.isUserLocked(TENANT_TEST, "testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds(TENANT_TEST, "testuser")).thenReturn(0L);

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setRole("SALES");

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    /**
     * ?updateUser - ?ownerScope?SALES ?     */
    @Test
    void testUpdateUser_Success_UpdateOwnerScopeForSales() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        user.setOwnerScope("oldscope");
        when(userAccountRepository.findByUsernameAndTenantId("testuser", TENANT_TEST)).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loginRiskService.isUserLocked(TENANT_TEST, "testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds(TENANT_TEST, "testuser")).thenReturn(0L);

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setOwnerScope("newscope");

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    /**
     * ?updateUser - ?SALES ?ownerScope ?
     */
    @Test
    void testUpdateUser_Success_OwnerScopeIgnoredForNonSales() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "ADMIN", true);
        user.setOwnerScope("adminscope");
        when(userAccountRepository.findByUsernameAndTenantId("testuser", TENANT_TEST)).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loginRiskService.isUserLocked(TENANT_TEST, "testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds(TENANT_TEST, "testuser")).thenReturn(0L);

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setOwnerScope("newscope");

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // ADMIN ?ownerScope ?        verify(userAccountRepository).save(any(UserAccount.class));
    }

    /**
     * ?updateUser - ?enabled ?     */
    @Test
    void testUpdateUser_Success_UpdateEnabled() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        when(userAccountRepository.findByUsernameAndTenantId("testuser", TENANT_TEST)).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loginRiskService.isUserLocked(TENANT_TEST, "testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds(TENANT_TEST, "testuser")).thenReturn(0L);

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setEnabled(false);

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    /**
     * ?updateUser - SALES ?ownerScope ?     */
    @Test
    void testUpdateUser_Success_AutoSetOwnerScopeForSales() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        user.setOwnerScope(null);
        when(userAccountRepository.findByUsernameAndTenantId("testuser", TENANT_TEST)).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> {
            UserAccount saved = inv.getArgument(0);
            assertEquals("testuser", saved.getOwnerScope());
            return saved;
        });
        when(loginRiskService.isUserLocked(TENANT_TEST, "testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds(TENANT_TEST, "testuser")).thenReturn(0L);

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setEnabled(true);

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    /**
     * ?updateUser - ?SALES ?ADMIN ?ownerScope
     */
    @Test
    void testUpdateUser_Success_ClearOwnerScopeWhenNotSales() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        user.setOwnerScope("testscope");
        when(userAccountRepository.findByUsernameAndTenantId("testuser", TENANT_TEST)).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> {
            UserAccount saved = inv.getArgument(0);
            assertEquals("", saved.getOwnerScope());
            return saved;
        });
        when(loginRiskService.isUserLocked(TENANT_TEST, "testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds(TENANT_TEST, "testuser")).thenReturn(0L);

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setRole("ADMIN");

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    /**
     * ?updateUser - ?
     */
    @Test
    void testUpdateUser_Success_RoleCaseInsensitive() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        when(userAccountRepository.findByUsernameAndTenantId("testuser", TENANT_TEST)).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loginRiskService.isUserLocked(TENANT_TEST, "testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds(TENANT_TEST, "testuser")).thenReturn(0L);

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setRole("manager"); // 闂傚倷娴囬褏鎹㈤幇顔藉床闁归偊鍠楀畷鏌ユ煙閻楀牊绶查柣?

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ==================== unlockUser ?====================

    /**
     * ?unlockUser - ?     */
    @Test
    void testUnlockUser_Forbidden() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("MANAGER");

        // Act
        ResponseEntity<?> response = adminUserController.unlockUser(request, "testuser");

        // Assert
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCodeValue());
        verify(loginRiskService, never()).clearUser(anyString(), anyString());
    }

    /**
     * ?unlockUser - ?     */
    @Test
    void testUnlockUser_UserNotFound() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(userAccountRepository.findByUsernameAndTenantId("nonexistent", TENANT_TEST)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = adminUserController.unlockUser(request, "nonexistent");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCodeValue());
        verify(loginRiskService, never()).clearUser(anyString(), anyString());
    }

    /**
     * ?unlockUser - ?
     */
    @Test
    void testUnlockUser_Success() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        when(userAccountRepository.findByUsernameAndTenantId("testuser", TENANT_TEST)).thenReturn(Optional.of(user));
        when(loginRiskService.isUserLocked(TENANT_TEST, "testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds(TENANT_TEST, "testuser")).thenReturn(0L);

        // Act
        ResponseEntity<?> response = adminUserController.unlockUser(request, "testuser");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(loginRiskService).clearUser(TENANT_TEST, "testuser");
        verify(auditLogService).record(eq("admin"), eq("ADMIN"), eq("UNLOCK"), eq("USER"), eq("testuser"), anyString(), eq(TENANT_TEST));
    }

    // ==================== toView ?===================

    /**
     * ?toView - ownerScope ?null ?     */
    @Test
    void testToView_NullOwnerScope() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        user.setOwnerScope(null);
        when(userAccountRepository.findAllByTenantIdOrderByUsernameAsc(TENANT_TEST)).thenReturn(Arrays.asList(user));
        when(loginRiskService.isUserLocked(TENANT_TEST, "testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds(TENANT_TEST, "testuser")).thenReturn(0L);

        // Act
        ResponseEntity<?> response = adminUserController.listUsers(request);

        // Assert
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertEquals("", items.get(0).get("ownerScope"));
    }

    /**
     * ?toView - enabled ?null ?false
     */
    @Test
    void testToView_NullEnabled() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        UserAccount user = createUser("testuser", "Test User", "SALES", null);
        when(userAccountRepository.findAllByTenantIdOrderByUsernameAsc(TENANT_TEST)).thenReturn(Arrays.asList(user));
        when(loginRiskService.isUserLocked(TENANT_TEST, "testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds(TENANT_TEST, "testuser")).thenReturn(0L);

        // Act
        ResponseEntity<?> response = adminUserController.listUsers(request);

        // Assert
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertFalse((Boolean) items.get(0).get("enabled"));
    }

    // ==================== ?====================

    private UserAccount createUser(String username, String displayName, String role, Boolean enabled) {
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setEnabled(enabled);
        user.setPassword("hashedpassword");
        user.setTenantId(TENANT_TEST);
        return user;
    }
}



