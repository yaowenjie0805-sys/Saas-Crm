package com.yao.crm.service;

import com.yao.crm.entity.Customer;
import com.yao.crm.entity.FollowUp;
import com.yao.crm.entity.Opportunity;
import com.yao.crm.entity.TaskItem;
import com.yao.crm.entity.UserAccount;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.FollowUpRepository;
import com.yao.crm.repository.OpportunityRepository;
import com.yao.crm.repository.TaskRepository;
import com.yao.crm.repository.UserAccountRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final CustomerRepository customerRepository;
    private final OpportunityRepository opportunityRepository;
    private final TaskRepository taskRepository;
    private final FollowUpRepository followUpRepository;
    private final UserAccountRepository userAccountRepository;
    private final ValueNormalizerService valueNormalizerService;

    public ReportService(CustomerRepository customerRepository,
                         OpportunityRepository opportunityRepository,
                         TaskRepository taskRepository,
                         FollowUpRepository followUpRepository,
                         UserAccountRepository userAccountRepository,
                         ValueNormalizerService valueNormalizerService) {
        this.customerRepository = customerRepository;
        this.opportunityRepository = opportunityRepository;
        this.taskRepository = taskRepository;
        this.followUpRepository = followUpRepository;
        this.userAccountRepository = userAccountRepository;
        this.valueNormalizerService = valueNormalizerService;
    }

    public Map<String, Object> overview(LocalDate fromDate, LocalDate toDate, String role) {
        return overviewByTenant("tenant_default", fromDate, toDate, role, "", "");
    }

    public Map<String, Object> overviewByTenant(String tenantId, LocalDate fromDate, LocalDate toDate, String role, String owner, String department) {
        Set<String> roleIdentities = loadRoleIdentities(tenantId, role);

        final String ownerFilter = owner == null ? "" : owner.trim().toLowerCase();
        final String deptFilter = department == null ? "" : department.trim().toLowerCase();

        List<Customer> customers = customerRepository.findByTenantId(tenantId).stream()
                .filter(c -> inDateRange(c.getCreatedAt(), fromDate, toDate))
                .filter(c -> matchRoleIdentity(c.getOwner(), roleIdentities))
                .filter(c -> ownerFilter.isEmpty() || (!isBlank(c.getOwner()) && ownerFilter.equals(c.getOwner().trim().toLowerCase())))
                .collect(Collectors.toList());

        List<Opportunity> opportunities = opportunityRepository.findByTenantId(tenantId).stream()
                .filter(o -> inDateRange(o.getCreatedAt(), fromDate, toDate))
                .filter(o -> matchRoleIdentity(o.getOwner(), roleIdentities))
                .filter(o -> ownerFilter.isEmpty() || (!isBlank(o.getOwner()) && ownerFilter.equals(o.getOwner().trim().toLowerCase())))
                .collect(Collectors.toList());

        List<TaskItem> tasks = taskRepository.findByTenantId(tenantId).stream()
                .filter(t -> inDateRange(t.getCreatedAt(), fromDate, toDate))
                .filter(t -> matchRoleIdentity(t.getOwner(), roleIdentities))
                .filter(t -> ownerFilter.isEmpty() || (!isBlank(t.getOwner()) && ownerFilter.equals(t.getOwner().trim().toLowerCase())))
                .collect(Collectors.toList());

        List<FollowUp> followUps = followUpRepository.findByTenantId(tenantId).stream()
                .filter(f -> inDateRange(f.getCreatedAt(), fromDate, toDate))
                .filter(f -> matchRoleIdentity(f.getAuthor(), roleIdentities))
                .filter(f -> ownerFilter.isEmpty() || (!isBlank(f.getAuthor()) && ownerFilter.equals(f.getAuthor().trim().toLowerCase())))
                .collect(Collectors.toList());

        Set<String> departmentIdentities = loadDepartmentIdentities(tenantId, deptFilter);
        if (!departmentIdentities.isEmpty()) {
            customers = customers.stream().filter(c -> departmentIdentities.contains(normalized(c.getOwner()))).collect(Collectors.toList());
            opportunities = opportunities.stream().filter(o -> departmentIdentities.contains(normalized(o.getOwner()))).collect(Collectors.toList());
            tasks = tasks.stream().filter(t -> departmentIdentities.contains(normalized(t.getOwner()))).collect(Collectors.toList());
            followUps = followUps.stream().filter(f -> departmentIdentities.contains(normalized(f.getAuthor()))).collect(Collectors.toList());
        }

        long totalRevenue = 0L;
        Map<String, Integer> customerByOwner = new HashMap<String, Integer>();
        for (Customer customer : customers) {
            totalRevenue += customer.getValue() == null ? 0 : customer.getValue();
            String ownerName = isBlank(customer.getOwner()) ? "Unknown" : customer.getOwner();
            customerByOwner.put(ownerName, customerByOwner.containsKey(ownerName) ? customerByOwner.get(ownerName) + 1 : 1);
        }

        Map<String, Long> revenueByStatus = new HashMap<String, Long>();
        for (Customer customer : customers) {
            String status = isBlank(customer.getStatus()) ? "Unknown" : customer.getStatus();
            Long value = customer.getValue() == null ? 0L : customer.getValue();
            revenueByStatus.put(status, revenueByStatus.containsKey(status) ? revenueByStatus.get(status) + value : value);
        }

        Map<String, Integer> opportunityByStage = new HashMap<String, Integer>();
        long weightedAmount = 0L;
        int highProgressCount = 0;
        for (Opportunity opportunity : opportunities) {
            String stage = isBlank(opportunity.getStage()) ? "Unknown" : opportunity.getStage();
            opportunityByStage.put(stage, opportunityByStage.containsKey(stage) ? opportunityByStage.get(stage) + 1 : 1);
            int progress = opportunity.getProgress() == null ? 0 : opportunity.getProgress();
            long amount = opportunity.getAmount() == null ? 0L : opportunity.getAmount();
            weightedAmount += Math.round(amount * (progress / 100.0));
            if (progress >= 80) highProgressCount++;
        }

        int doneTasks = 0;
        int pendingTasks = 0;
        for (TaskItem task : tasks) {
            if (Boolean.TRUE.equals(task.getDone())) doneTasks++; else pendingTasks++;
        }

        Map<String, Integer> followUpByChannel = new HashMap<String, Integer>();
        for (FollowUp followUp : followUps) {
            String channel = isBlank(followUp.getChannel()) ? "Unknown" : valueNormalizerService.normalizeFollowUpChannel(followUp.getChannel());
            followUpByChannel.put(channel, followUpByChannel.containsKey(channel) ? followUpByChannel.get(channel) + 1 : 1);
        }

        double winRate = opportunities.isEmpty() ? 0.0 : Math.round((highProgressCount * 1000.0 / opportunities.size())) / 10.0;
        double taskDoneRate = tasks.isEmpty() ? 0.0 : Math.round((doneTasks * 1000.0 / tasks.size())) / 10.0;

        Map<String, Object> summary = new HashMap<String, Object>();
        summary.put("customers", customers.size());
        summary.put("revenue", totalRevenue);
        summary.put("opportunities", opportunities.size());
        summary.put("weightedAmount", weightedAmount);
        summary.put("winRate", winRate);
        summary.put("taskDoneRate", taskDoneRate);
        summary.put("followUps", followUps.size());

        Map<String, Object> body = new HashMap<String, Object>();
        body.put("summary", summary);
        body.put("customerByOwner", customerByOwner);
        body.put("revenueByStatus", revenueByStatus);
        body.put("opportunityByStage", opportunityByStage);
        Map<String, Integer> taskStatus = new HashMap<String, Integer>();
        taskStatus.put("done", doneTasks);
        taskStatus.put("pending", pendingTasks);
        body.put("taskStatus", taskStatus);
        body.put("followUpByChannel", followUpByChannel);
        return body;
    }

    public String exportOverviewCsv(LocalDate fromDate, LocalDate toDate, String role) {
        Map<String, Object> report = overviewByTenant("tenant_default", fromDate, toDate, role, "", "");
        return toCsv(report, fromDate, toDate, role);
    }

    public String exportOverviewCsvByTenant(String tenantId, LocalDate fromDate, LocalDate toDate, String role, String owner, String department) {
        Map<String, Object> report = overviewByTenant(tenantId, fromDate, toDate, role, owner, department);
        return toCsv(report, fromDate, toDate, role);
    }

    private String toCsv(Map<String, Object> report, LocalDate fromDate, LocalDate toDate, String role) {
        Map<String, Object> summary = castMap(report.get("summary"));
        Map<String, Integer> taskStatus = castMap(report.get("taskStatus"));

        StringBuilder sb = new StringBuilder();
        sb.append("section,key,value\n");
        sb.append(row("filter", "from", fromDate == null ? "" : fromDate.toString()));
        sb.append(row("filter", "to", toDate == null ? "" : toDate.toString()));
        sb.append(row("filter", "role", role == null ? "" : role.trim().toUpperCase()));

        appendMapRows(sb, "summary", summary);
        appendMapRows(sb, "customerByOwner", castMap(report.get("customerByOwner")));
        appendMapRows(sb, "revenueByStatus", castMap(report.get("revenueByStatus")));
        appendMapRows(sb, "opportunityByStage", castMap(report.get("opportunityByStage")));
        appendMapRows(sb, "taskStatus", taskStatus);
        appendMapRows(sb, "followUpByChannel", castMap(report.get("followUpByChannel")));
        return sb.toString();
    }

    private void appendMapRows(StringBuilder sb, String section, Map<String, ?> map) {
        if (map == null) {
            return;
        }
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            sb.append(row(section, entry.getKey(), entry.getValue()));
        }
    }

    private String row(String section, String key, Object value) {
        return csv(section) + "," + csv(key) + "," + csv(value == null ? "" : String.valueOf(value)) + "\n";
    }

    private String csv(String text) {
        String safe = text == null ? "" : text;
        String escaped = safe.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    @SuppressWarnings("unchecked")
    private <T> Map<String, T> castMap(Object obj) {
        if (!(obj instanceof Map)) {
            return new HashMap<String, T>();
        }
        return (Map<String, T>) obj;
    }

    private boolean inDateRange(LocalDateTime createdAt, LocalDate from, LocalDate to) {
        if (createdAt == null) return false;
        LocalDate date = createdAt.toLocalDate();
        if (from != null && date.isBefore(from)) return false;
        if (to != null && date.isAfter(to)) return false;
        return true;
    }

    private Set<String> loadRoleIdentities(String tenantId, String role) {
        if (isBlank(role)) return null;
        String roleUpper = role.trim().toUpperCase();
        Set<String> identities = new HashSet<String>();
        for (UserAccount user : userAccountRepository.findAll()) {
            if (!tenantId.equals(user.getTenantId())) continue;
            if (user.getRole() != null && roleUpper.equalsIgnoreCase(user.getRole())) {
                if (!isBlank(user.getUsername())) identities.add(user.getUsername().trim().toLowerCase());
                if (!isBlank(user.getDisplayName())) identities.add(user.getDisplayName().trim().toLowerCase());
            }
        }
        return identities;
    }

    private Set<String> loadDepartmentIdentities(String tenantId, String department) {
        Set<String> identities = new HashSet<String>();
        if (isBlank(department)) return identities;
        for (UserAccount user : userAccountRepository.findAll()) {
            if (!tenantId.equals(user.getTenantId())) continue;
            if (isBlank(user.getDepartment()) || !department.equals(user.getDepartment().trim().toLowerCase())) continue;
            if (!isBlank(user.getUsername())) identities.add(normalized(user.getUsername()));
            if (!isBlank(user.getDisplayName())) identities.add(normalized(user.getDisplayName()));
        }
        return identities;
    }

    private boolean matchRoleIdentity(String text, Set<String> identities) {
        if (identities == null) return true;
        if (isBlank(text)) return false;
        return identities.contains(normalized(text));
    }

    private String normalized(String text) {
        return isBlank(text) ? "" : text.trim().toLowerCase();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
