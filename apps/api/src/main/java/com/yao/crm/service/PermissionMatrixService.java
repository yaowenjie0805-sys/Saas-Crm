package com.yao.crm.service;

import com.yao.crm.enums.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PermissionMatrixService {

    private final List<String> roles = Arrays.asList(
            UserRole.ADMIN.name(),
            UserRole.MANAGER.name(),
            UserRole.SALES.name(),
            UserRole.ANALYST.name()
    );
    private final List<String> operations = Arrays.asList(
            "opViewDashboard",
            "opViewReports",
            "opManageCustomers",
            "opDeleteCustomers",
            "opManageTasks",
            "opManageFollowUps",
            "opCreateOpportunity",
            "opEditOpportunityAmount"
    );

    private final Map<String, Set<String>> byRole = new ConcurrentHashMap<String, Set<String>>();
    private final Map<String, Deque<Set<String>>> history = new ConcurrentHashMap<String, Deque<Set<String>>>();

    public PermissionMatrixService() {
        byRole.put(UserRole.ADMIN.name(), new HashSet<String>(operations));
        byRole.put(UserRole.MANAGER.name(), new HashSet<String>(operations));
        byRole.put(UserRole.SALES.name(), new HashSet<String>(Arrays.asList(
                "opViewDashboard",
                "opManageCustomers",
                "opManageTasks",
                "opManageFollowUps",
                "opCreateOpportunity"
        )));
        byRole.put(UserRole.ANALYST.name(), new HashSet<String>(Arrays.asList(
                "opViewDashboard",
                "opViewReports"
        )));
        for (String role : roles) {
            history.put(role, new ArrayDeque<Set<String>>());
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> matrixRows() {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (String operation : operations) {
            Map<String, Object> row = new HashMap<String, Object>();
            row.put("key", operation);
            List<String> allowedRoles = new ArrayList<String>();
            for (String role : roles) {
                if (hasPermission(role, operation)) {
                    allowedRoles.add(role);
                }
            }
            row.put("roles", allowedRoles);
            rows.add(row);
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> roleView(String role) {
        String normalized = normalizeRole(role);
        return roleViewBySet(normalized, byRole.get(normalized));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> previewRole(String role, List<String> grant, List<String> revoke) {
        String normalized = normalizeRole(role);
        Set<String> snapshot = new HashSet<String>(byRole.get(normalized));
        applyMutation(snapshot, grant, revoke);
        Map<String, Object> body = roleViewBySet(normalized, snapshot);
        body.put("preview", true);
        return body;
    }

    @Transactional(timeout = 30)
    public Map<String, Object> updateRole(String role, List<String> grant, List<String> revoke) {
        String normalized = normalizeRole(role);
        Set<String> current = byRole.get(normalized);
        pushHistory(normalized, current);
        applyMutation(current, grant, revoke);

        Map<String, Object> body = roleView(normalized);
        body.put("matrix", matrixRows());
        return body;
    }

    @Transactional(timeout = 30)
    public Map<String, Object> rollbackRole(String role) {
        String normalized = normalizeRole(role);
        Deque<Set<String>> stack = history.get(normalized);
        if (stack == null || stack.isEmpty()) {
            throw new IllegalStateException("permission_rollback_empty");
        }
        byRole.put(normalized, new HashSet<String>(stack.pop()));

        Map<String, Object> body = roleView(normalized);
        body.put("matrix", matrixRows());
        body.put("rolledBack", true);
        return body;
    }

    @Transactional(readOnly = true)
    public List<Map<String, String>> conflicts() {
        List<Map<String, String>> rows = new ArrayList<Map<String, String>>();
        for (String role : roles) {
            List<String> warnings = validateRole(role, byRole.get(role));
            for (String warning : warnings) {
                Map<String, String> row = new HashMap<String, String>();
                row.put("role", role);
                row.put("message", warning);
                rows.add(row);
            }
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public List<String> roles() {
        return roles;
    }

    private void pushHistory(String role, Set<String> current) {
        Deque<Set<String>> stack = history.get(role);
        if (stack == null) {
            stack = new ArrayDeque<Set<String>>();
            history.put(role, stack);
        }
        stack.push(new HashSet<String>(current));
        while (stack.size() > 10) {
            stack.removeLast();
        }
    }

    private void applyMutation(Set<String> target, List<String> grant, List<String> revoke) {
        for (String op : grant == null ? Collections.<String>emptyList() : grant) {
            if (!operations.contains(op)) {
                throw new IllegalArgumentException("invalid_permission");
            }
            target.add(op);
        }
        for (String op : revoke == null ? Collections.<String>emptyList() : revoke) {
            if (!operations.contains(op)) {
                throw new IllegalArgumentException("invalid_permission");
            }
            target.remove(op);
        }
    }

    private Map<String, Object> roleViewBySet(String role, Set<String> permissions) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("role", role);
        body.put("permissions", new ArrayList<String>(permissions));
        body.put("conflicts", validateRole(role, permissions));
        return body;
    }

    private boolean hasPermission(String role, String operation) {
        Set<String> set = byRole.get(role);
        return set != null && set.contains(operation);
    }

    private List<String> validateRole(String role, Set<String> set) {
        List<String> warnings = new ArrayList<String>();
        if (set == null) {
            return warnings;
        }

        if (set.contains("opDeleteCustomers") && !set.contains("opManageCustomers")) {
            warnings.add("delete_requires_manage_customers");
        }
        if (set.contains("opEditOpportunityAmount") && !set.contains("opCreateOpportunity")) {
            warnings.add("edit_amount_requires_create_opportunity");
        }

        if (UserRole.ANALYST.name().equals(role)) {
            if (set.contains("opManageCustomers") || set.contains("opDeleteCustomers") || set.contains("opManageTasks")
                    || set.contains("opManageFollowUps") || set.contains("opCreateOpportunity") || set.contains("opEditOpportunityAmount")) {
                warnings.add("analyst_should_be_read_only");
            }
        }

        if (UserRole.SALES.name().equals(role)) {
            if (set.contains("opDeleteCustomers") || set.contains("opEditOpportunityAmount")) {
                warnings.add("sales_should_not_have_high_risk_write");
            }
        }

        return warnings;
    }

    private String normalizeRole(String role) {
        if (role == null) {
            throw new IllegalArgumentException("invalid_role");
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (!roles.contains(normalized)) {
            throw new IllegalArgumentException("invalid_role");
        }
        return normalized;
    }
}