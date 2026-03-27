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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AdminUserController 单元测试
 * 
 * 测试覆盖以下场景：
 * 1. listUsers - 列出所有用户
 * 2. updateUser - 更新用户信息（角色、ownerScope、enabled）
 * 3. unlockUser - 解锁用户
 * 
 * 每个方法都测试了权限检查、用户不存在、参数验证等分支
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
        // 设置 I18nService 默认行为
        when(i18nService.msg(any(HttpServletRequest.class), anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(1);
            return key;
        });
    }

    // ==================== listUsers 测试 ====================

    /**
     * 测试 listUsers - 无权限访问
     * 验证非 ADMIN 角色无法访问用户列表
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
    }

    /**
     * 测试 listUsers - 成功获取空列表
     */
    @Test
    void testListUsers_Success_EmptyList() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(userAccountRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<?> response = adminUserController.listUsers(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertTrue(items.isEmpty());
        verify(userAccountRepository).findAll();
    }

    /**
     * 测试 listUsers - 成功获取用户列表并按用户名排序
     */
    @Test
    void testListUsers_Success_WithUsers() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");

        UserAccount user1 = createUser("zack", "Zack", "ADMIN", true);
        UserAccount user2 = createUser("alice", "Alice", "SALES", true);
        UserAccount user3 = createUser("bob", "Bob", "MANAGER", false);

        when(userAccountRepository.findAll()).thenReturn(Arrays.asList(user1, user2, user3));
        when(loginRiskService.isUserLocked(anyString())).thenReturn(false);
        when(loginRiskService.remainingUserSeconds(anyString())).thenReturn(0L);

        // Act
        ResponseEntity<?> response = adminUserController.listUsers(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertEquals(3, items.size());
        // 验证按用户名排序
        assertEquals("alice", items.get(0).get("username"));
        assertEquals("bob", items.get(1).get("username"));
        assertEquals("zack", items.get(2).get("username"));
    }

    /**
     * 测试 listUsers - 用户被锁定的情况
     */
    @Test
    void testListUsers_Success_WithLockedUser() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");

        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        when(userAccountRepository.findAll()).thenReturn(Arrays.asList(user));
        when(loginRiskService.isUserLocked("testuser")).thenReturn(true);
        when(loginRiskService.remainingUserSeconds("testuser")).thenReturn(300L);

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

    // ==================== updateUser 测试 ====================

    /**
     * 测试 updateUser - 无权限访问
     */
    @Test
    void testUpdateUser_Forbidden() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("SALES");
        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCodeValue());
        verify(userAccountRepository, never()).findByUsername(anyString());
    }

    /**
     * 测试 updateUser - 用户不存在
     */
    @Test
    void testUpdateUser_UserNotFound() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(userAccountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setRole("ADMIN");

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "nonexistent", payload);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCodeValue());
    }

    /**
     * 测试 updateUser - 无效角色
     */
    @Test
    void testUpdateUser_InvalidRole() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setRole("INVALID_ROLE");

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    /**
     * 测试 updateUser - 成功更新角色为 ADMIN
     */
    @Test
    void testUpdateUser_Success_UpdateRoleToAdmin() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        user.setOwnerScope("testuser");
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loginRiskService.isUserLocked("testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds("testuser")).thenReturn(0L);

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setRole("ADMIN");

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userAccountRepository).save(any(UserAccount.class));
        verify(auditLogService).record(eq("admin"), eq("ADMIN"), eq("UPDATE"), eq("USER"), eq("testuser"), anyString());
    }

    /**
     * 测试 updateUser - 成功更新角色为 SALES（保留 ownerScope）
     */
    @Test
    void testUpdateUser_Success_UpdateRoleToSales() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "ADMIN", true);
        user.setOwnerScope("");
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loginRiskService.isUserLocked("testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds("testuser")).thenReturn(0L);

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setRole("SALES");

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    /**
     * 测试 updateUser - 成功更新 ownerScope（仅 SALES 角色有效）
     */
    @Test
    void testUpdateUser_Success_UpdateOwnerScopeForSales() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        user.setOwnerScope("oldscope");
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loginRiskService.isUserLocked("testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds("testuser")).thenReturn(0L);

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setOwnerScope("newscope");

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    /**
     * 测试 updateUser - 非 SALES 角色更新 ownerScope 无效
     */
    @Test
    void testUpdateUser_Success_OwnerScopeIgnoredForNonSales() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "ADMIN", true);
        user.setOwnerScope("adminscope");
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loginRiskService.isUserLocked("testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds("testuser")).thenReturn(0L);

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setOwnerScope("newscope");

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // ADMIN 角色的 ownerScope 不应被更新
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    /**
     * 测试 updateUser - 成功更新 enabled 状态
     */
    @Test
    void testUpdateUser_Success_UpdateEnabled() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loginRiskService.isUserLocked("testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds("testuser")).thenReturn(0L);

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setEnabled(false);

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    /**
     * 测试 updateUser - SALES 角色且 ownerScope 为空时自动设置为用户名
     */
    @Test
    void testUpdateUser_Success_AutoSetOwnerScopeForSales() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        user.setOwnerScope(null);
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> {
            UserAccount saved = inv.getArgument(0);
            assertEquals("testuser", saved.getOwnerScope());
            return saved;
        });
        when(loginRiskService.isUserLocked("testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds("testuser")).thenReturn(0L);

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setEnabled(true);

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    /**
     * 测试 updateUser - 角色从 SALES 改为 ADMIN 时清空 ownerScope
     */
    @Test
    void testUpdateUser_Success_ClearOwnerScopeWhenNotSales() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        user.setOwnerScope("testscope");
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> {
            UserAccount saved = inv.getArgument(0);
            assertEquals("", saved.getOwnerScope());
            return saved;
        });
        when(loginRiskService.isUserLocked("testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds("testuser")).thenReturn(0L);

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setRole("ADMIN");

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    /**
     * 测试 updateUser - 角色大小写不敏感
     */
    @Test
    void testUpdateUser_Success_RoleCaseInsensitive() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loginRiskService.isUserLocked("testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds("testuser")).thenReturn(0L);

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setRole("manager"); // 小写

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ==================== unlockUser 测试 ====================

    /**
     * 测试 unlockUser - 无权限访问
     */
    @Test
    void testUnlockUser_Forbidden() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("MANAGER");

        // Act
        ResponseEntity<?> response = adminUserController.unlockUser(request, "testuser");

        // Assert
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCodeValue());
        verify(loginRiskService, never()).clearUser(anyString());
    }

    /**
     * 测试 unlockUser - 用户不存在
     */
    @Test
    void testUnlockUser_UserNotFound() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(userAccountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = adminUserController.unlockUser(request, "nonexistent");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCodeValue());
        verify(loginRiskService, never()).clearUser(anyString());
    }

    /**
     * 测试 unlockUser - 成功解锁用户
     */
    @Test
    void testUnlockUser_Success() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(loginRiskService.isUserLocked("testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds("testuser")).thenReturn(0L);

        // Act
        ResponseEntity<?> response = adminUserController.unlockUser(request, "testuser");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(loginRiskService).clearUser("testuser");
        verify(auditLogService).record(eq("admin"), eq("ADMIN"), eq("UNLOCK"), eq("USER"), eq("testuser"), anyString());
    }

    // ==================== toView 私有方法测试（通过公开方法间接测试）====================

    /**
     * 测试 toView - ownerScope 为 null 时返回空字符串
     */
    @Test
    void testToView_NullOwnerScope() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        user.setOwnerScope(null);
        when(userAccountRepository.findAll()).thenReturn(Arrays.asList(user));
        when(loginRiskService.isUserLocked("testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds("testuser")).thenReturn(0L);

        // Act
        ResponseEntity<?> response = adminUserController.listUsers(request);

        // Assert
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertEquals("", items.get(0).get("ownerScope"));
    }

    /**
     * 测试 toView - enabled 为 null 时返回 false
     */
    @Test
    void testToView_NullEnabled() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        UserAccount user = createUser("testuser", "Test User", "SALES", null);
        when(userAccountRepository.findAll()).thenReturn(Arrays.asList(user));
        when(loginRiskService.isUserLocked("testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds("testuser")).thenReturn(0L);

        // Act
        ResponseEntity<?> response = adminUserController.listUsers(request);

        // Assert
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertFalse((Boolean) items.get(0).get("enabled"));
    }

    // ==================== 辅助方法 ====================

    private UserAccount createUser(String username, String displayName, String role, Boolean enabled) {
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setEnabled(enabled);
        user.setPassword("hashedpassword");
        user.setTenantId("tenant_default");
        return user;
    }
}
