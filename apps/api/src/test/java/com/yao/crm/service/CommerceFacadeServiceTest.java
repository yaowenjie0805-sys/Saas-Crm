package com.yao.crm.service;

import com.yao.crm.entity.OrderRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class CommerceFacadeServiceTest {

    @Test
    void shouldNormalizeStatusAndClampPageSize() {
        CommerceFacadeService facade = new CommerceFacadeService(null, null, null, null);
        Set<String> allowed = new HashSet<String>(Arrays.asList("DRAFT", "APPROVED"));

        Assertions.assertEquals("DRAFT", facade.normalizeStatusOrBlank(" draft ", allowed));
        Assertions.assertEquals("", facade.normalizeStatusOrBlank("  ", allowed));
        Assertions.assertNull(facade.normalizeStatusOrBlank("invalid", allowed));
        Assertions.assertEquals(1, facade.normalizePageSize(0));
        Assertions.assertEquals(100, facade.normalizePageSize(999));
    }

    @Test
    void shouldResolveOrderApprovalModeFromNotesWithoutTenantLookup() {
        CommerceFacadeService facade = new CommerceFacadeService(null, null, null, null);
        OrderRecord order = new OrderRecord();
        order.setNotes("created [approval_mode=stage_gate]");

        Assertions.assertEquals("STAGE_GATE", facade.resolveOrderApprovalMode(order, "t_1"));
        Assertions.assertTrue(facade.buildOrderCreationNotes("Q-1", "STRICT").contains("[approval_mode=STRICT]"));
    }
}

