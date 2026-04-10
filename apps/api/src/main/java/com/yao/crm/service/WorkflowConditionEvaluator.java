package com.yao.crm.service;

import com.yao.crm.enums.ConditionOperator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class WorkflowConditionEvaluator {

    public boolean evaluateConditions(List<Map<String, Object>> conditions,
                                      Map<String, Object> triggerData,
                                      Map<String, Object> variables,
                                      String logic) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        String mode = logic == null ? "AND" : logic.trim().toUpperCase(Locale.ROOT);
        if (!"AND".equals(mode) && !"OR".equals(mode)) {
            mode = "AND";
        }
        for (Map<String, Object> condition : conditions) {
            String field = (String) condition.get("field");
            String operator = (String) condition.get("operator");
            Object value = condition.get("value");

            Object fieldValue = getFieldValue(field, triggerData, variables);
            boolean conditionResult = evaluateSingleCondition(fieldValue, operator, value);

            if ("AND".equals(mode) && !conditionResult) {
                return false;
            }
            if ("OR".equals(mode) && conditionResult) {
                return true;
            }
        }
        return "OR".equals(mode) ? false : true;
    }

    public boolean evaluateSingleCondition(Object fieldValue, String operator, Object compareValue) {
        ConditionOperator op = ConditionOperator.fromString(operator);
        if (fieldValue == null) {
            return op == ConditionOperator.IS_NULL || op == ConditionOperator.IS_EMPTY;
        }

        if (op == null) {
            return false;
        }

        String fieldText = fieldValue.toString();
        String compareText = compareValue == null ? "" : compareValue.toString();
        if (compareValue == null) {
            switch (op) {
                case EQUALS:
                case CONTAINS:
                case STARTS_WITH:
                case ENDS_WITH:
                case GREATER_THAN:
                case LESS_THAN:
                case GREATER_EQUAL:
                case LESS_EQUAL:
                    return false;
                case NOT_EQUALS:
                case NOT_CONTAINS:
                    return true;
                default:
                    break;
            }
        }

        switch (op) {
            case EQUALS:
                return fieldText.equals(compareText);
            case NOT_EQUALS:
                return !fieldText.equals(compareText);
            case CONTAINS:
                return fieldText.contains(compareText);
            case NOT_CONTAINS:
                return !fieldText.contains(compareText);
            case STARTS_WITH:
                return fieldText.startsWith(compareText);
            case ENDS_WITH:
                return fieldText.endsWith(compareText);
            case GREATER_THAN:
                return compareNumbers(fieldValue, compareValue) > 0;
            case LESS_THAN:
                return compareNumbers(fieldValue, compareValue) < 0;
            case GREATER_EQUAL:
                return compareNumbers(fieldValue, compareValue) >= 0;
            case LESS_EQUAL:
                return compareNumbers(fieldValue, compareValue) <= 0;
            case IS_NULL:
            case IS_EMPTY:
                return fieldValue == null || fieldText.isEmpty();
            case IS_NOT_NULL:
            case IS_NOT_EMPTY:
                return fieldValue != null && !fieldText.isEmpty();
            default:
                return false;
        }
    }

    private int compareNumbers(Object a, Object b) {
        try {
            double numA = Double.parseDouble(a.toString());
            double numB = Double.parseDouble(b.toString());
            return Double.compare(numA, numB);
        } catch (Exception e) {
            String aText = a == null ? "" : a.toString();
            String bText = b == null ? "" : b.toString();
            return aText.compareTo(bText);
        }
    }

    private Object getFieldValue(String field, Map<String, Object> triggerData, Map<String, Object> variables) {
        if (field == null) {
            return null;
        }
        if (field.startsWith("{{") && field.endsWith("}}")) {
            String varName = field.substring(2, field.length() - 2).trim();
            Object variable = variables == null ? null : variables.get(varName);
            if (variable != null) {
                return variable;
            }
            return triggerData == null ? null : triggerData.get(varName);
        }
        Object variable = variables == null ? null : variables.get(field);
        if (variable != null) {
            return variable;
        }
        return triggerData == null ? null : triggerData.get(field);
    }
}
