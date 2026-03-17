package com.yao.crm.service;

import com.yao.crm.entity.Customer;
import com.yao.crm.entity.FollowUp;
import com.yao.crm.entity.Opportunity;
import com.yao.crm.entity.OrderRecord;
import com.yao.crm.entity.PaymentRecord;
import com.yao.crm.entity.Quote;
import com.yao.crm.entity.TaskItem;
import com.yao.crm.entity.UserAccount;
import com.yao.crm.entity.Lead;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.FollowUpRepository;
import com.yao.crm.repository.OpportunityRepository;
import com.yao.crm.repository.OrderRecordRepository;
import com.yao.crm.repository.PaymentRecordRepository;
import com.yao.crm.repository.QuoteRepository;
import com.yao.crm.repository.TaskRepository;
import com.yao.crm.repository.UserAccountRepository;
import com.yao.crm.repository.LeadRepository;
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
    private final QuoteRepository quoteRepository;
    private final OrderRecordRepository orderRecordRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final UserAccountRepository userAccountRepository;
    private final LeadRepository leadRepository;
    private final ValueNormalizerService valueNormalizerService;

    public ReportService(CustomerRepository customerRepository,
                         OpportunityRepository opportunityRepository,
                         TaskRepository taskRepository,
                         FollowUpRepository followUpRepository,
                         QuoteRepository quoteRepository,
                         OrderRecordRepository orderRecordRepository,
                         PaymentRecordRepository paymentRecordRepository,
                         UserAccountRepository userAccountRepository,
                         LeadRepository leadRepository,
                         ValueNormalizerService valueNormalizerService) {
        this.customerRepository = customerRepository;
        this.opportunityRepository = opportunityRepository;
        this.taskRepository = taskRepository;
        this.followUpRepository = followUpRepository;
        this.quoteRepository = quoteRepository;
        this.orderRecordRepository = orderRecordRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.userAccountRepository = userAccountRepository;
        this.leadRepository = leadRepository;
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
        List<Quote> quotes = quoteRepository.findByTenantId(tenantId).stream()
                .filter(qt -> inDateRange(qt.getCreatedAt(), fromDate, toDate))
                .filter(qt -> matchRoleIdentity(qt.getOwner(), roleIdentities))
                .filter(qt -> ownerFilter.isEmpty() || (!isBlank(qt.getOwner()) && ownerFilter.equals(qt.getOwner().trim().toLowerCase())))
                .collect(Collectors.toList());
        List<OrderRecord> orders = orderRecordRepository.findByTenantId(tenantId).stream()
                .filter(or -> inDateRange(or.getCreatedAt(), fromDate, toDate))
                .filter(or -> matchRoleIdentity(or.getOwner(), roleIdentities))
                .filter(or -> ownerFilter.isEmpty() || (!isBlank(or.getOwner()) && ownerFilter.equals(or.getOwner().trim().toLowerCase())))
                .collect(Collectors.toList());
        List<PaymentRecord> payments = paymentRecordRepository.findByTenantId(tenantId).stream()
                .filter(pr -> inDateRange(pr.getCreatedAt(), fromDate, toDate))
                .filter(pr -> matchRoleIdentity(pr.getOwner(), roleIdentities))
                .filter(pr -> ownerFilter.isEmpty() || (!isBlank(pr.getOwner()) && ownerFilter.equals(pr.getOwner().trim().toLowerCase())))
                .collect(Collectors.toList());

        Set<String> departmentIdentities = loadDepartmentIdentities(tenantId, deptFilter);
        if (!departmentIdentities.isEmpty()) {
            customers = customers.stream().filter(c -> departmentIdentities.contains(normalized(c.getOwner()))).collect(Collectors.toList());
            opportunities = opportunities.stream().filter(o -> departmentIdentities.contains(normalized(o.getOwner()))).collect(Collectors.toList());
            tasks = tasks.stream().filter(t -> departmentIdentities.contains(normalized(t.getOwner()))).collect(Collectors.toList());
            followUps = followUps.stream().filter(f -> departmentIdentities.contains(normalized(f.getAuthor()))).collect(Collectors.toList());
            quotes = quotes.stream().filter(qt -> departmentIdentities.contains(normalized(qt.getOwner()))).collect(Collectors.toList());
            orders = orders.stream().filter(or -> departmentIdentities.contains(normalized(or.getOwner()))).collect(Collectors.toList());
            payments = payments.stream().filter(pr -> departmentIdentities.contains(normalized(pr.getOwner()))).collect(Collectors.toList());
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
        long quoteApproved = quotes.stream().filter(q -> "APPROVED".equalsIgnoreCase(q.getStatus()) || "ACCEPTED".equalsIgnoreCase(q.getStatus())).count();
        long quoteAccepted = quotes.stream().filter(q -> "ACCEPTED".equalsIgnoreCase(q.getStatus())).count();
        long orderCompleted = orders.stream().filter(o -> "COMPLETED".equalsIgnoreCase(o.getStatus())).count();
        double quoteApproveRate = quotes.isEmpty() ? 0.0 : Math.round((quoteApproved * 1000.0 / quotes.size())) / 10.0;
        double quoteToOrderRate = quoteAccepted == 0 ? 0.0 : Math.round((orders.size() * 1000.0 / quoteAccepted)) / 10.0;
        double orderCompleteRate = orders.isEmpty() ? 0.0 : Math.round((orderCompleted * 1000.0 / orders.size())) / 10.0;
        summary.put("quotes", quotes.size());
        summary.put("orders", orders.size());
        summary.put("quoteApproveRate", quoteApproveRate);
        summary.put("quoteToOrderRate", quoteToOrderRate);
        summary.put("orderCompleteRate", orderCompleteRate);
        Set<String> orderIds = orders.stream().map(OrderRecord::getId).collect(Collectors.toSet());
        long orderAmountTotal = orders.stream().mapToLong(o -> o.getAmount() == null ? 0L : o.getAmount()).sum();
        long orderPaymentReceived = payments.stream()
                .filter(p -> !isBlank(p.getOrderId()) && orderIds.contains(p.getOrderId()) && "RECEIVED".equalsIgnoreCase(p.getStatus()))
                .mapToLong(p -> p.getAmount() == null ? 0L : p.getAmount())
                .sum();
        long orderPaymentOutstanding = Math.max(0L, orderAmountTotal - orderPaymentReceived);
        double orderCollectionRate = orderAmountTotal <= 0 ? 0.0 : Math.round((orderPaymentReceived * 1000.0 / orderAmountTotal)) / 10.0;
        summary.put("orderPaymentReceived", orderPaymentReceived);
        summary.put("orderPaymentOutstanding", orderPaymentOutstanding);
        summary.put("orderCollectionRate", orderCollectionRate);

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
        Map<String, Integer> quoteByStatus = new HashMap<String, Integer>();
        for (Quote quote : quotes) {
            String key = isBlank(quote.getStatus()) ? "Unknown" : quote.getStatus();
            quoteByStatus.put(key, quoteByStatus.containsKey(key) ? quoteByStatus.get(key) + 1 : 1);
        }
        Map<String, Integer> orderByStatus = new HashMap<String, Integer>();
        for (OrderRecord order : orders) {
            String key = isBlank(order.getStatus()) ? "Unknown" : order.getStatus();
            orderByStatus.put(key, orderByStatus.containsKey(key) ? orderByStatus.get(key) + 1 : 1);
        }
        body.put("quoteByStatus", quoteByStatus);
        body.put("orderByStatus", orderByStatus);
        return body;
    }

    public String exportOverviewCsv(LocalDate fromDate, LocalDate toDate, String role) {
        return exportOverviewCsvByTenant("tenant_default", fromDate, toDate, role, "", "", "en");
    }

    public Map<String, Object> funnelByTenant(String tenantId, LocalDate fromDate, LocalDate toDate, String owner) {
        final String ownerFilter = owner == null ? "" : owner.trim().toLowerCase();
        List<Lead> leads = leadRepository.findByTenantId(tenantId).stream()
                .filter(l -> inDateRange(l.getCreatedAt(), fromDate, toDate))
                .filter(l -> ownerFilter.isEmpty() || (!isBlank(l.getOwner()) && ownerFilter.equals(l.getOwner().trim().toLowerCase())))
                .collect(Collectors.toList());
        List<Opportunity> opportunities = opportunityRepository.findByTenantId(tenantId).stream()
                .filter(o -> inDateRange(o.getCreatedAt(), fromDate, toDate))
                .filter(o -> ownerFilter.isEmpty() || (!isBlank(o.getOwner()) && ownerFilter.equals(o.getOwner().trim().toLowerCase())))
                .collect(Collectors.toList());
        List<Quote> quotes = quoteRepository.findByTenantId(tenantId).stream()
                .filter(q -> inDateRange(q.getCreatedAt(), fromDate, toDate))
                .filter(q -> ownerFilter.isEmpty() || (!isBlank(q.getOwner()) && ownerFilter.equals(q.getOwner().trim().toLowerCase())))
                .collect(Collectors.toList());
        List<OrderRecord> orders = orderRecordRepository.findByTenantId(tenantId).stream()
                .filter(o -> inDateRange(o.getCreatedAt(), fromDate, toDate))
                .filter(o -> ownerFilter.isEmpty() || (!isBlank(o.getOwner()) && ownerFilter.equals(o.getOwner().trim().toLowerCase())))
                .collect(Collectors.toList());

        long leadsCount = leads.size();
        long oppCount = opportunities.size();
        long quoteCount = quotes.size();
        long orderCount = orders.size();

        double leadToOpp = leadsCount == 0 ? 0.0 : Math.round((oppCount * 1000.0 / leadsCount)) / 10.0;
        double oppToQuote = oppCount == 0 ? 0.0 : Math.round((quoteCount * 1000.0 / oppCount)) / 10.0;
        double quoteToOrder = quoteCount == 0 ? 0.0 : Math.round((orderCount * 1000.0 / quoteCount)) / 10.0;

        Map<String, Object> out = new HashMap<String, Object>();
        Map<String, Object> counts = new HashMap<String, Object>();
        counts.put("leads", leadsCount);
        counts.put("opportunities", oppCount);
        counts.put("quotes", quoteCount);
        counts.put("orders", orderCount);
        out.put("counts", counts);

        Map<String, Object> rates = new HashMap<String, Object>();
        rates.put("leadToOpportunity", leadToOpp);
        rates.put("opportunityToQuote", oppToQuote);
        rates.put("quoteToOrder", quoteToOrder);
        out.put("rates", rates);
        return out;
    }

    public String exportOverviewCsvByTenant(String tenantId, LocalDate fromDate, LocalDate toDate, String role, String owner, String department) {
        return exportOverviewCsvByTenant(tenantId, fromDate, toDate, role, owner, department, "en");
    }

    public String exportOverviewCsvByTenant(String tenantId,
                                            LocalDate fromDate,
                                            LocalDate toDate,
                                            String role,
                                            String owner,
                                            String department,
                                            String language) {
        Map<String, Object> report = overviewByTenant(tenantId, fromDate, toDate, role, owner, department);
        return toCsv(report, fromDate, toDate, role, language);
    }

    private String toCsv(Map<String, Object> report, LocalDate fromDate, LocalDate toDate, String role, String language) {
        Map<String, Object> summary = castMap(report.get("summary"));
        Map<String, Integer> taskStatus = castMap(report.get("taskStatus"));
        boolean zh = language != null && language.trim().toLowerCase().startsWith("zh");

        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append(zh ? "分组,字段,值\n" : "section,key,value\n");
        sb.append(row(zh ? "筛选" : "filter", zh ? "开始" : "from", fromDate == null ? "" : fromDate.toString()));
        sb.append(row(zh ? "筛选" : "filter", zh ? "结束" : "to", toDate == null ? "" : toDate.toString()));
        sb.append(row(zh ? "筛选" : "filter", zh ? "角色" : "role", role == null ? "" : role.trim().toUpperCase()));

        appendMapRows(sb, zh ? "汇总" : "summary", summary);
        appendMapRows(sb, zh ? "客户负责人分布" : "customerByOwner", castMap(report.get("customerByOwner")));
        appendMapRows(sb, zh ? "客户状态营收" : "revenueByStatus", castMap(report.get("revenueByStatus")));
        appendMapRows(sb, zh ? "商机阶段分布" : "opportunityByStage", castMap(report.get("opportunityByStage")));
        appendMapRows(sb, zh ? "任务状态" : "taskStatus", taskStatus);
        appendMapRows(sb, zh ? "跟进渠道分布" : "followUpByChannel", castMap(report.get("followUpByChannel")));
        appendMapRows(sb, zh ? "报价状态分布" : "quoteByStatus", castMap(report.get("quoteByStatus")));
        appendMapRows(sb, zh ? "订单状态分布" : "orderByStatus", castMap(report.get("orderByStatus")));
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

