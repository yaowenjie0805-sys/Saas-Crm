package com.yao.crm.controller;

import com.yao.crm.dto.request.AdminUpdateUserRequest;
import com.yao.crm.dto.request.V1InviteUserRequest;
import com.yao.crm.entity.UserAccount;
import com.yao.crm.entity.UserInvitation;
import com.yao.crm.repository.UserAccountRepository;
import com.yao.crm.repository.UserInvitationRepository;
import com.yao.crm.security.LoginRiskService;
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

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class V1AdminUserControllerTest {

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private UserInvitationRepository userInvitationRepository;
    @Mock
    private LoginRiskService loginRiskService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private I18nService i18nService;

    private V1AdminUserController controller;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        controller = new V1AdminUserController(
                userAccountRepository,
                userInvitationRepository,
                loginRiskService,
                auditLogService,
                i18nService
        );
        when(i18nService.msg(any(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        request = new MockHttpServletRequest();
        request.setAttribute("authRole", "ADMIN");
        request.setAttribute("authUsername", "admin-1");
        request.setAttribute("authTenantId", "tenant-1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void patchUserShouldTrimIdAndNormalizeRoleBeforeSave() {
        UserAccount user = user("u-1", "tenant-1", "target");
        user.setRole("SALES");
        when(userAccountRepository.findById("u-1")).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setRole(" manager ");

        ResponseEntity<?> response = controller.patchUser(request, "  u-1  ", payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userAccountRepository).findById("u-1");
        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(captor.capture());
        assertEquals("MANAGER", captor.getValue().getRole());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("user_updated", body.get("code"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void patchUserShouldReturnIdRequiredWhenIdBlankAfterNormalization() {
        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setEnabled(Boolean.TRUE);

        ResponseEntity<?> response = controller.patchUser(request, "   ", payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("id_required", body.get("code"));
        verifyNoInteractions(userAccountRepository);
    }

    @Test
    void unlockUserShouldTrimIdBeforeLookup() {
        UserAccount user = user("u-2", "tenant-1", "alice");
        when(userAccountRepository.findById("u-2")).thenReturn(Optional.of(user));

        ResponseEntity<?> response = controller.unlockUser(request, "  u-2  ");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userAccountRepository).findById("u-2");
        verify(loginRiskService).clearUser("alice");
    }

    @Test
    @SuppressWarnings("unchecked")
    void inviteUserShouldNormalizeRoleScopeAndUsernameBeforeSave() {
        when(userAccountRepository.findByUsernameAndTenantId("alice", "tenant-1")).thenReturn(Optional.empty());
        when(userInvitationRepository.save(any(UserInvitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        V1InviteUserRequest payload = invitePayload();
        payload.setUsername("  alice  ");
        payload.setRole(" sales ");
        payload.setDataScope(" all ");
        payload.setOwnerScope("  owner-1  ");

        ResponseEntity<?> response = controller.inviteUser(request, payload);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(userAccountRepository).findByUsernameAndTenantId("alice", "tenant-1");
        ArgumentCaptor<UserInvitation> captor = ArgumentCaptor.forClass(UserInvitation.class);
        verify(userInvitationRepository).save(captor.capture());
        assertEquals("alice", captor.getValue().getUsername());
        assertEquals("SALES", captor.getValue().getRole());
        assertEquals("GLOBAL", captor.getValue().getDataScope());
        assertEquals("owner-1", captor.getValue().getOwnerScope());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("invitation_created", body.get("code"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void inviteUserShouldReturnBadRequestWhenUsernameBlankAfterNormalization() {
        V1InviteUserRequest payload = invitePayload();
        payload.setUsername("   ");

        ResponseEntity<?> response = controller.inviteUser(request, payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("register_username_required", body.get("code"));
        verifyNoInteractions(userInvitationRepository);
        verify(userAccountRepository, never()).findByUsernameAndTenantId(anyString(), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void inviteUserShouldReturnConflictWhenUsernameAlreadyExists() {
        UserAccount existing = user("u-exist", "tenant-1", "alice");
        when(userAccountRepository.findByUsernameAndTenantId("alice", "tenant-1")).thenReturn(Optional.of(existing));

        V1InviteUserRequest payload = invitePayload();
        payload.setUsername("  alice  ");

        ResponseEntity<?> response = controller.inviteUser(request, payload);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("username_exists", body.get("code"));
        verify(userAccountRepository).findByUsernameAndTenantId("alice", "tenant-1");
        verify(userInvitationRepository, never()).save(any(UserInvitation.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void inviteUserShouldReturnBadRequestWhenScopeInvalidAfterNormalization() {
        V1InviteUserRequest payload = invitePayload();
        payload.setDataScope("invalid_scope");

        ResponseEntity<?> response = controller.inviteUser(request, payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("bad_request", body.get("code"));
        verify(userAccountRepository, never()).findByUsernameAndTenantId(anyString(), anyString());
        verify(userInvitationRepository, never()).save(any(UserInvitation.class));
    }

    private V1InviteUserRequest invitePayload() {
        V1InviteUserRequest payload = new V1InviteUserRequest();
        payload.setUsername("new-user");
        payload.setRole("SALES");
        payload.setOwnerScope("new-user");
        payload.setDepartment("DEFAULT");
        payload.setDataScope("SELF");
        payload.setDisplayName("New User");
        return payload;
    }

    private UserAccount user(String id, String tenantId, String username) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setDisplayName(username);
        user.setRole("SALES");
        user.setOwnerScope(username);
        user.setEnabled(Boolean.TRUE);
        user.setPassword("pwd");
        return user;
    }
}
