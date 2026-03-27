package com.yao.crm.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * MDC Task Decorator for propagating MDC context to async threads.
 * Ensures traceId, tenantId, userId are available in async task logging.
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // Capture the current MDC context from the calling thread
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        
        return new Runnable() {
            @Override
            public void run() {
                // Set the captured MDC context in the async thread
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                try {
                    runnable.run();
                } finally {
                    // Clean up MDC after task completion
                    MDC.clear();
                }
            }
        };
    }
}
