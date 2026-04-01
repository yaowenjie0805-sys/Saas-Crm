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

class DualMarketDemoDataInitializerSeedUserPasswordTest {

    @Test
    void upsertUserShouldNotOverridePasswordForExistingUser() {
        DualMarketDemoDataInitializer initializer = new DualMarketDemoDataInitializer();
        UserAccountRepository repository = mock(UserAccountRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        UserAccount existing = new UserAccount();
        existing.setId("existing-id");
        existing.setUsername("cn_admin");
        existing.setPassword("existing-hash");
        existing.setTenantId("tenant_cn_demo");
        when(repository.findByUsernameAndTenantId("cn_admin", "tenant_cn_demo")).thenReturn(Optional.of(existing));

        ReflectionTestUtils.invokeMethod(
                initializer,
                "upsertUser",
                repository,
                encoder,
                "u_cn_admin",
                "cn_admin",
                "seed-password",
                "ADMIN",
                "CN Admin",
                "",
                "tenant_cn_demo"
        );

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository).save(captor.capture());
        verify(encoder, never()).encode(anyString());
        assertEquals("existing-hash", captor.getValue().getPassword());
    }

    @Test
    void upsertUserShouldSetPasswordWhenCreatingUser() {
        DualMarketDemoDataInitializer initializer = new DualMarketDemoDataInitializer();
        UserAccountRepository repository = mock(UserAccountRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        when(repository.findByUsernameAndTenantId("cn_admin", "tenant_cn_demo")).thenReturn(Optional.empty());
        when(encoder.encode("seed-password")).thenReturn("encoded-seed-password");

        ReflectionTestUtils.invokeMethod(
                initializer,
                "upsertUser",
                repository,
                encoder,
                "u_cn_admin",
                "cn_admin",
                "seed-password",
                "ADMIN",
                "CN Admin",
                "",
                "tenant_cn_demo"
        );

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository).save(captor.capture());
        verify(encoder).encode("seed-password");
        assertEquals("encoded-seed-password", captor.getValue().getPassword());
    }
}

