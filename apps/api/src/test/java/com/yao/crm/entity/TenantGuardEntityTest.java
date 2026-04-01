package com.yao.crm.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantGuardEntityTest {

    @Test
    void customerShouldFailFastWhenTenantIsBlank() {
        Customer customer = new Customer();
        customer.setTenantId("   ");

        IllegalStateException error = assertThrows(IllegalStateException.class, customer::prePersist);

        assertEquals("tenant_id_required", error.getMessage());
    }

    @Test
    void userAccountShouldFailFastWhenTenantIsMissing() {
        UserAccount userAccount = new UserAccount();
        userAccount.setTenantId(null);

        IllegalStateException error = assertThrows(IllegalStateException.class, userAccount::prePersist);

        assertEquals("tenant_id_required", error.getMessage());
    }

    @Test
    void workflowDefinitionShouldFailFastWhenTenantIsBlank() {
        WorkflowDefinition workflowDefinition = new WorkflowDefinition();
        workflowDefinition.setTenantId("  ");

        IllegalStateException error = assertThrows(IllegalStateException.class, workflowDefinition::prePersist);

        assertEquals("tenant_id_required", error.getMessage());
    }

    @Test
    void customerPreUpdateShouldNotSilentlyBackfillTenantDefault() {
        Customer customer = new Customer();
        customer.setTenantId("   ");

        customer.preUpdate();

        assertEquals("   ", customer.getTenantId());
    }

    @Test
    void workflowDefinitionPreUpdateShouldNotSilentlyBackfillTenantDefault() {
        WorkflowDefinition workflowDefinition = new WorkflowDefinition();
        workflowDefinition.setTenantId(" ");

        workflowDefinition.preUpdate();

        assertEquals(" ", workflowDefinition.getTenantId());
    }
}
