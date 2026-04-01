package com.yao.crm.config;

import com.yao.crm.entity.UserAccount;
import com.yao.crm.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataInitializerSeedUserPasswordTest {

    @Test
    void upsertUserShouldNotOverridePasswordForExistingUserWhenResetDisabled() {
        DataInitializer initializer = new DataInitializer();
        ReflectionTestUtils.setField(initializer, "seedTenantId", "tenant_test");
        ReflectionTestUtils.setField(initializer, "resetBootstrapPassword", false);
        UserAccountRepository repository = mock(UserAccountRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        UserAccount existing = new UserAccount();
        existing.setId("existing-id");
        existing.setUsername("admin");
        existing.setPassword("existing-hash");
        existing.setTenantId("tenant_test");
        when(repository.findByUsernameAndTenantId("admin", "tenant_test")).thenReturn(Optional.of(existing));

        ReflectionTestUtils.invokeMethod(
                initializer,
                "upsertUser",
                repository,
                encoder,
                "u_admin",
                "admin",
                "seed-password",
                "ADMIN",
                "System Admin",
                ""
        );

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository).save(captor.capture());
        verify(encoder, never()).encode(anyString());
        assertEquals("existing-hash", captor.getValue().getPassword());
    }

    @Test
    void upsertUserShouldResetPasswordForExistingUserWhenResetEnabled() {
        DataInitializer initializer = new DataInitializer();
        ReflectionTestUtils.setField(initializer, "seedTenantId", "tenant_test");
        ReflectionTestUtils.setField(initializer, "resetBootstrapPassword", true);
        UserAccountRepository repository = mock(UserAccountRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        UserAccount existing = new UserAccount();
        existing.setId("existing-id");
        existing.setUsername("admin");
        existing.setPassword("existing-hash");
        existing.setTenantId("tenant_test");
        when(repository.findByUsernameAndTenantId("admin", "tenant_test")).thenReturn(Optional.of(existing));
        when(encoder.encode("seed-password")).thenReturn("encoded-seed-password");

        ReflectionTestUtils.invokeMethod(
                initializer,
                "upsertUser",
                repository,
                encoder,
                "u_admin",
                "admin",
                "seed-password",
                "ADMIN",
                "System Admin",
                ""
        );

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository).save(captor.capture());
        verify(encoder).encode("seed-password");
        assertEquals("encoded-seed-password", captor.getValue().getPassword());
    }

    @Test
    void upsertUserShouldSetPasswordWhenCreatingUser() {
        DataInitializer initializer = new DataInitializer();
        ReflectionTestUtils.setField(initializer, "seedTenantId", "tenant_test");
        ReflectionTestUtils.setField(initializer, "resetBootstrapPassword", false);
        UserAccountRepository repository = mock(UserAccountRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        when(repository.findByUsernameAndTenantId("admin", "tenant_test")).thenReturn(Optional.empty());
        when(encoder.encode("seed-password")).thenReturn("encoded-seed-password");

        ReflectionTestUtils.invokeMethod(
                initializer,
                "upsertUser",
                repository,
                encoder,
                "u_admin",
                "admin",
                "seed-password",
                "ADMIN",
                "System Admin",
                ""
        );

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository).save(captor.capture());
        verify(encoder).encode("seed-password");
        assertEquals("encoded-seed-password", captor.getValue().getPassword());
    }
}
