package com.yao.crm.service;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowActionExecutorTest {

    private final WorkflowActionExecutor executor = new WorkflowActionExecutor();

    @Test
    void execute_UpdateField_ShouldMutateVariables() {
        Map<String, Object> config = new HashMap<>();
        config.put("fieldName", "stage");
        config.put("fieldValue", "CLOSED");
        Map<String, Object> variables = new HashMap<>();
        variables.put("stage", "OPEN");

        Map<String, Object> output = executor.execute("UPDATE_FIELD", config, variables);

        assertEquals("CLOSED", variables.get("stage"));
        assertEquals("stage", output.get("updatedField"));
        assertEquals("CLOSED", output.get("newValue"));
    }

    @Test
    void execute_LowercaseActionType_ShouldStillWork() {
        Map<String, Object> config = new HashMap<>();
        config.put("taskName", "My Task");

        Map<String, Object> output = executor.execute("create_task", config, new HashMap<>());

        assertEquals("My Task", output.get("taskName"));
        assertNotNull(output.get("taskId"));
    }

    @Test
    void execute_UpdateFieldWithBlankFieldName_ShouldNotWriteNullKey() {
        Map<String, Object> config = new HashMap<>();
        config.put("fieldName", " ");
        config.put("fieldValue", 1);
        Map<String, Object> variables = new HashMap<>();

        Map<String, Object> output = executor.execute("UPDATE_FIELD", config, variables);

        assertFalse(variables.containsKey(null));
        assertFalse(variables.containsKey(""));
        assertTrue(output.isEmpty());
    }
}
