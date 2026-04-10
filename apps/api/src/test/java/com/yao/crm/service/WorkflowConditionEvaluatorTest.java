package com.yao.crm.service;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowConditionEvaluatorTest {

    private final WorkflowConditionEvaluator evaluator = new WorkflowConditionEvaluator();

    @Test
    void evaluateConditions_InvalidLogic_ShouldFallbackToAnd() {
        Map<String, Object> c1 = new HashMap<>();
        c1.put("field", "stage");
        c1.put("operator", "EQUALS");
        c1.put("value", "OPEN");

        Map<String, Object> c2 = new HashMap<>();
        c2.put("field", "priority");
        c2.put("operator", "EQUALS");
        c2.put("value", "P1");

        Map<String, Object> variables = new HashMap<>();
        variables.put("stage", "OPEN");
        variables.put("priority", "P2");

        boolean result = evaluator.evaluateConditions(
                List.of(c1, c2),
                new HashMap<>(),
                variables,
                "XOR"
        );

        assertFalse(result);
    }

    @Test
    void evaluateConditions_OrLogic_ShouldReturnTrueWhenAnyConditionMatches() {
        Map<String, Object> c1 = new HashMap<>();
        c1.put("field", "stage");
        c1.put("operator", "EQUALS");
        c1.put("value", "OPEN");

        Map<String, Object> c2 = new HashMap<>();
        c2.put("field", "priority");
        c2.put("operator", "EQUALS");
        c2.put("value", "P1");

        Map<String, Object> variables = new HashMap<>();
        variables.put("stage", "OPEN");
        variables.put("priority", "P2");

        boolean result = evaluator.evaluateConditions(
                List.of(c1, c2),
                new HashMap<>(),
                variables,
                "OR"
        );

        assertTrue(result);
    }

    @Test
    void evaluateConditions_PlainField_ShouldFallbackToTriggerDataWhenVariableMissing() {
        Map<String, Object> condition = new HashMap<>();
        condition.put("field", "source");
        condition.put("operator", "EQUALS");
        condition.put("value", "WEB");

        Map<String, Object> triggerData = new HashMap<>();
        triggerData.put("source", "WEB");

        boolean result = evaluator.evaluateConditions(
                List.of(condition),
                triggerData,
                new HashMap<>(),
                "AND"
        );

        assertTrue(result);
    }

    @Test
    void evaluateSingleCondition_NullFieldWithLowercaseIsNull_ShouldBeTrue() {
        boolean result = evaluator.evaluateSingleCondition(null, "is_null", null);

        assertTrue(result);
    }
}
