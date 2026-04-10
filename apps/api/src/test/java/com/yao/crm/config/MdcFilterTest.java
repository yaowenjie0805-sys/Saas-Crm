package com.yao.crm.config;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MdcFilterTest {

    @Test
    void shouldGenerateServerTraceIdWhenHeaderIsInvalid() throws Exception {
        MdcFilter filter = new MdcFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("X-Trace-Id", "bad trace id");

        AtomicReference<String> traceRef = new AtomicReference<>();
        FilterChain chain = (req, res) -> traceRef.set(MDC.get("traceId"));

        filter.doFilter(request, response, chain);

        String traceId = traceRef.get();
        assertNotNull(traceId);
        assertTrue(traceId.matches("[a-f0-9]{32}"));
    }

    @Test
    void shouldSanitizeTenantIdWhenHeaderContainsIllegalChars() throws Exception {
        MdcFilter filter = new MdcFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("X-Tenant-Id", "tenant test");

        AtomicReference<String> tenantRef = new AtomicReference<>();
        FilterChain chain = (req, res) -> tenantRef.set(MDC.get("tenantId"));

        filter.doFilter(request, response, chain);

        assertEquals("unknown", tenantRef.get());
    }
}

