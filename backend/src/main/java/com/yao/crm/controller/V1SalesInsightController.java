package com.yao.crm.controller;

import com.yao.crm.entity.ApprovalInstance;
import com.yao.crm.entity.ContractRecord;
import com.yao.crm.entity.FollowUp;
import com.yao.crm.entity.OrderRecord;
import com.yao.crm.entity.PaymentRecord;
import com.yao.crm.entity.Quote;
import com.yao.crm.entity.TaskItem;
import com.yao.crm.repository.ApprovalInstanceRepository;
import com.yao.crm.repository.ContractRecordRepository;
import com.yao.crm.repository.FollowUpRepository;
import com.yao.crm.repository.OrderRecordRepository;
import com.yao.crm.repository.PaymentRecordRepository;
import com.yao.crm.repository.QuoteRepository;
import com.yao.crm.repository.TaskRepository;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.OpportunityRepository;
import com.yao.crm.service.I18nService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class V1SalesInsightController extends BaseApiController {

    private final TaskRepository taskRepository;
    private final FollowUpRepository followUpRepository;
    private final ContractRecordRepository contractRecordRepository;
    private final ApprovalInstanceRepository approvalInstanceRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final CustomerRepository customerRepository;
    private final OpportunityRepository opportunityRepository;
    private final QuoteRepository quoteRepository;
    private final OrderRecordRepository orderRecordRepository;

    public V1SalesInsightController(TaskRepository taskRepository,
                                    FollowUpRepository followUpRepository,
                                    ContractRecordRepository contractRecordRepository,
                                    ApprovalInstanceRepository approvalInstanceRepository,
                                    PaymentRecordRepository paymentRecordRepository,
                                    CustomerRepository customerRepository,
                                    OpportunityRepository opportunityRepository,
                                    QuoteRepository quoteRepository,
                                    OrderRecordRepository orderRecordRepository,
                                    I18nService i18nService) {
        super(i18nService);
        this.taskRepository = taskRepository;
        this.followUpRepository = followUpRepository;
        this.contractRecordRepository = contractRecordRepository;
        this.approvalInstanceRepository = approvalInstanceRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.customerRepository = customerRepository;
        this.opportunityRepository = opportunityRepository;
        this.quoteRepository = quoteRepository;
        this.orderRecordRepository = orderRecordRepository;
    }

    @GetMapping("/workbench/today")
    public ResponseEntity<?> workbenchToday(HttpServletRequest request,
                                            @RequestParam(defaultValue = "") String from,
                                            @RequestParam(defaultValue = "") String to,
                                            @RequestParam(defaultValue = "") String owner,
                                            @RequestParam(defaultValue = "") String department,
                                            @RequestParam(defaultValue = "") String timezone) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        LocalDate today = LocalDate.now();
        LocalDateTime fromAt = parseDateStart(request, from);
        LocalDateTime toAt = parseDateEnd(request, to);
        String ownerFilter = isBlank(owner) ? "" : owner.trim();

        List<TaskItem> tasks = taskRepository.findByTenantId(tenantId).stream()
                .filter(row -> matchScopedOwner(request, row.getOwner()))
                .filter(row -> matchesOwnerFilter(row.getOwner(), ownerFilter))
                .filter(row -> inRange(row.getUpdatedAt(), fromAt, toAt))
                .collect(Collectors.toList());
        List<FollowUp> followUps = followUpRepository.findByTenantId(tenantId).stream()
                .filter(row -> matchScopedOwner(request, row.getAuthor()))
                .filter(row -> matchesOwnerFilter(row.getAuthor(), ownerFilter))
                .filter(row -> inRange(row.getUpdatedAt(), fromAt, toAt))
                .collect(Collectors.toList());
        List<ContractRecord> contracts = contractRecordRepository.findByTenantId(tenantId).stream()
                .filter(row -> matchScopedOwner(request, row.getOwner()))
                .filter(row -> matchesOwnerFilter(row.getOwner(), ownerFilter))
                .filter(row -> inRange(row.getUpdatedAt(), fromAt, toAt))
                .collect(Collectors.toList());
        List<ApprovalInstance> approvals = approvalInstanceRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .filter(row -> matchScopedOwner(request, row.getSubmitter()))
                .filter(row -> matchesOwnerFilter(row.getSubmitter(), ownerFilter))
                .filter(row -> inRange(row.getUpdatedAt(), fromAt, toAt))
                .collect(Collectors.toList());
        List<PaymentRecord> payments = paymentRecordRepository.findByTenantId(tenantId).stream()
                .filter(row -> matchScopedOwner(request, row.getOwner()))
                .filter(row -> matchesOwnerFilter(row.getOwner(), ownerFilter))
                .filter(row -> inRange(row.getUpdatedAt(), fromAt, toAt))
                .collect(Collectors.toList());

        int todoTasks = (int) tasks.stream().filter(t -> !Boolean.TRUE.equals(t.getDone())).count();
        int overdueFollowUps = (int) followUps.stream().filter(f -> f.getNextActionDate() != null && f.getNextActionDate().isBefore(today)).count();
        int upcomingContracts = (int) contracts.stream().filter(c -> c.getSignDate() != null && !c.getSignDate().isBefore(today) && !c.getSignDate().isAfter(today.plusDays(15))).count();
        int pendingApprovals = (int) approvals.stream().filter(a -> "PENDING".equalsIgnoreCase(a.getStatus()) || "WAITING".equalsIgnoreCase(a.getStatus())).count();
        int paymentWarnings = (int) payments.stream().filter(p -> "OVERDUE".equalsIgnoreCase(p.getStatus()) || "PENDING".equalsIgnoreCase(p.getStatus())).count();

        List<Map<String, Object>> cards = new ArrayList<Map<String, Object>>();
        cards.add(card("todoTasks", "todayTodo", todoTasks, "info"));
        cards.add(card("overdueFollowUps", "overdueFollowUps", overdueFollowUps, overdueFollowUps > 0 ? "warning" : "info"));
        cards.add(card("upcomingContracts", "upcomingContracts", upcomingContracts, "info"));
        cards.add(card("pendingApprovals", "pendingApprovals", pendingApprovals, pendingApprovals > 0 ? "warning" : "info"));
        cards.add(card("paymentWarnings", "paymentWarnings", paymentWarnings, paymentWarnings > 0 ? "danger" : "info"));

        List<Map<String, Object>> todoItems = new ArrayList<Map<String, Object>>();
        for (TaskItem row : tasks) {
            if (Boolean.TRUE.equals(row.getDone())) continue;
            todoItems.add(workbenchItem("TASK", row.getId(), row.getTitle(), row.getOwner(), row.getUpdatedAt(), "tasks", row.getLevel(), null));
        }
        for (ApprovalInstance row : approvals) {
            String status = String.valueOf(row.getStatus() == null ? "" : row.getStatus()).toUpperCase(Locale.ROOT);
            if (!"PENDING".equals(status) && !"WAITING".equals(status)) continue;
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("status", "PENDING");
            payload.put("instanceId", row.getId());
            todoItems.add(workbenchItem("APPROVAL", row.getId(), row.getBizType() + ":" + row.getBizId(), row.getSubmitter(), row.getUpdatedAt(), "approvals", row.getStatus(), payload));
        }
        sortWorkbenchItems(todoItems);
        if (todoItems.size() > 12) {
            todoItems = new ArrayList<Map<String, Object>>(todoItems.subList(0, 12));
        }

        List<Map<String, Object>> warningItems = new ArrayList<Map<String, Object>>();
        for (FollowUp row : followUps) {
            if (row.getNextActionDate() == null || !row.getNextActionDate().isBefore(today)) continue;
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("customerId", row.getCustomerId());
            payload.put("q", row.getSummary());
            warningItems.add(workbenchItem("FOLLOW_UP", row.getId(), row.getSummary(), row.getAuthor(), row.getUpdatedAt(), "followUps", "OVERDUE", payload));
        }
        for (ContractRecord row : contracts) {
            if (row.getSignDate() == null || row.getSignDate().isBefore(today) || row.getSignDate().isAfter(today.plusDays(15))) continue;
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("status", row.getStatus());
            payload.put("contractId", row.getId());
            warningItems.add(workbenchItem("CONTRACT", row.getId(), row.getContractNo(), row.getOwner(), row.getUpdatedAt(), "contracts", row.getStatus(), payload));
        }
        for (PaymentRecord row : payments) {
            String status = String.valueOf(row.getStatus() == null ? "" : row.getStatus()).toUpperCase(Locale.ROOT);
            if (!"OVERDUE".equals(status) && !"PENDING".equals(status)) continue;
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("status", row.getStatus());
            payload.put("customerId", row.getCustomerId());
            warningItems.add(workbenchItem("PAYMENT", row.getId(), row.getId(), row.getOwner(), row.getUpdatedAt(), "payments", row.getStatus(), payload));
        }
        sortWorkbenchItems(warningItems);
        if (warningItems.size() > 12) {
            warningItems = new ArrayList<Map<String, Object>>(warningItems.subList(0, 12));
        }

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("date", today.toString());
        body.put("cards", cards);
        body.put("todoItems", todoItems);
        body.put("warningItems", warningItems);
        body.put("timezone", isBlank(timezone) ? "system" : timezone.trim());
        body.put("department", department == null ? "" : department.trim());
        body.put("owner", ownerFilter);
        body.put("from", from == null ? "" : from.trim());
        body.put("to", to == null ? "" : to.trim());
        body.put("updatedAt", LocalDateTime.now());
        return ResponseEntity.ok(successWithFields(request, "workbench_today_loaded", body));
    }

    @GetMapping("/customers/{id}/timeline")
    public ResponseEntity<?> customerTimeline(HttpServletRequest request, @PathVariable String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Optional<com.yao.crm.entity.Customer> customer = customerRepository.findByIdAndTenantId(id, tenantId);
        if (!customer.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "customer_not_found", msg(request, "customer_not_found"), null));
        }
        if (!matchScopedOwner(request, customer.get().getOwner())) {
            return ResponseEntity.status(403).body(errorBody(request, "scope_forbidden", msg(request, "scope_forbidden"), null));
        }
        List<Map<String, Object>> events = buildCustomerTimeline(tenantId, id);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("customerId", id);
        body.put("items", events);
        body.put("total", events.size());
        return ResponseEntity.ok(successWithFields(request, "timeline_loaded", body));
    }

    @GetMapping("/opportunities/{id}/timeline")
    public ResponseEntity<?> opportunityTimeline(HttpServletRequest request, @PathVariable String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Optional<com.yao.crm.entity.Opportunity> opportunity = opportunityRepository.findById(id).filter(o -> tenantId.equals(o.getTenantId()));
        if (!opportunity.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "opportunity_not_found", msg(request, "opportunity_not_found"), null));
        }
        if (!matchScopedOwner(request, opportunity.get().getOwner())) {
            return ResponseEntity.status(403).body(errorBody(request, "scope_forbidden", msg(request, "scope_forbidden"), null));
        }

        List<Quote> quotes = quoteRepository.findByTenantIdAndOpportunityId(tenantId, id);
        List<OrderRecord> orders = orderRecordRepository.findByTenantIdAndOpportunityId(tenantId, id);
        Set<String> quoteIds = quotes.stream().map(Quote::getId).collect(Collectors.toSet());
        Set<String> orderIds = orders.stream().map(OrderRecord::getId).collect(Collectors.toSet());
        List<ApprovalInstance> approvals = approvalInstanceRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .filter(a -> ("QUOTE".equalsIgnoreCase(a.getBizType()) && quoteIds.contains(a.getBizId()))
                        || ("ORDER".equalsIgnoreCase(a.getBizType()) && orderIds.contains(a.getBizId())))
                .collect(Collectors.toList());

        List<Map<String, Object>> events = new ArrayList<Map<String, Object>>();
        for (Quote q : quotes) {
            events.add(timelineEvent(q.getUpdatedAt(), "QUOTE", q.getQuoteNo(), q.getStatus(), q.getId()));
        }
        for (OrderRecord o : orders) {
            events.add(timelineEvent(o.getUpdatedAt(), "ORDER", o.getOrderNo(), o.getStatus(), o.getId()));
        }
        for (ApprovalInstance a : approvals) {
            events.add(timelineEvent(a.getUpdatedAt(), "APPROVAL", a.getBizType() + ":" + a.getBizId(), a.getStatus(), a.getId()));
        }
        sortTimeline(events);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("opportunityId", id);
        body.put("items", events);
        body.put("total", events.size());
        return ResponseEntity.ok(successWithFields(request, "timeline_loaded", body));
    }

    private List<Map<String, Object>> buildCustomerTimeline(String tenantId, String customerId) {
        List<Quote> quotes = quoteRepository.findByTenantId(tenantId).stream()
                .filter(q -> customerId.equals(q.getCustomerId()))
                .collect(Collectors.toList());
        List<OrderRecord> orders = orderRecordRepository.findByTenantId(tenantId).stream()
                .filter(o -> customerId.equals(o.getCustomerId()))
                .collect(Collectors.toList());
        List<ContractRecord> contracts = contractRecordRepository.findByTenantId(tenantId).stream()
                .filter(c -> customerId.equals(c.getCustomerId()))
                .collect(Collectors.toList());
        List<PaymentRecord> payments = paymentRecordRepository.findByTenantId(tenantId).stream()
                .filter(p -> customerId.equals(p.getCustomerId()))
                .collect(Collectors.toList());
        List<FollowUp> followUps = followUpRepository.findByTenantId(tenantId).stream()
                .filter(f -> customerId.equals(f.getCustomerId()))
                .collect(Collectors.toList());

        Set<String> contractIds = contracts.stream().map(ContractRecord::getId).collect(Collectors.toSet());
        Set<String> quoteIds = quotes.stream().map(Quote::getId).collect(Collectors.toSet());
        Set<String> orderIds = orders.stream().map(OrderRecord::getId).collect(Collectors.toSet());
        List<ApprovalInstance> approvals = approvalInstanceRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .filter(a -> ("CONTRACT".equalsIgnoreCase(a.getBizType()) && contractIds.contains(a.getBizId()))
                        || ("QUOTE".equalsIgnoreCase(a.getBizType()) && quoteIds.contains(a.getBizId()))
                        || ("ORDER".equalsIgnoreCase(a.getBizType()) && orderIds.contains(a.getBizId())))
                .collect(Collectors.toList());

        List<Map<String, Object>> events = new ArrayList<Map<String, Object>>();
        for (FollowUp f : followUps) {
            events.add(timelineEvent(f.getUpdatedAt(), "FOLLOW_UP", f.getSummary(), f.getChannel(), f.getId()));
        }
        for (Quote q : quotes) {
            events.add(timelineEvent(q.getUpdatedAt(), "QUOTE", q.getQuoteNo(), q.getStatus(), q.getId()));
        }
        for (OrderRecord o : orders) {
            events.add(timelineEvent(o.getUpdatedAt(), "ORDER", o.getOrderNo(), o.getStatus(), o.getId()));
        }
        for (ContractRecord c : contracts) {
            events.add(timelineEvent(c.getUpdatedAt(), "CONTRACT", c.getContractNo(), c.getStatus(), c.getId()));
        }
        for (PaymentRecord p : payments) {
            events.add(timelineEvent(p.getUpdatedAt(), "PAYMENT", p.getId(), p.getStatus(), p.getId()));
        }
        for (ApprovalInstance a : approvals) {
            events.add(timelineEvent(a.getUpdatedAt(), "APPROVAL", a.getBizType() + ":" + a.getBizId(), a.getStatus(), a.getId()));
        }
        sortTimeline(events);
        return events;
    }

    private void sortTimeline(List<Map<String, Object>> events) {
        Collections.sort(events, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> a, Map<String, Object> b) {
                LocalDateTime ta = (LocalDateTime) a.get("timeRaw");
                LocalDateTime tb = (LocalDateTime) b.get("timeRaw");
                if (ta == null && tb == null) return 0;
                if (ta == null) return 1;
                if (tb == null) return -1;
                return tb.compareTo(ta);
            }
        });
        for (Map<String, Object> row : events) {
            row.remove("timeRaw");
        }
    }

    private Map<String, Object> timelineEvent(LocalDateTime time, String type, String title, String status, String sourceId) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("time", time);
        item.put("timeRaw", time);
        item.put("type", type);
        item.put("title", title == null ? "-" : title);
        item.put("status", status == null ? "-" : status);
        item.put("sourceId", sourceId);
        return item;
    }

    private Map<String, Object> card(String key, String labelKey, long value, String level) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("key", key);
        item.put("labelKey", labelKey);
        item.put("value", value);
        item.put("level", level);
        return item;
    }

    private Map<String, Object> workbenchItem(String type,
                                              String id,
                                              String title,
                                              String owner,
                                              LocalDateTime updatedAt,
                                              String targetPage,
                                              String status,
                                              Map<String, Object> payload) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("type", type);
        item.put("id", id);
        item.put("title", title == null ? "-" : title);
        item.put("owner", owner == null ? "-" : owner);
        item.put("updatedAt", updatedAt);
        item.put("status", status == null ? "-" : status);
        item.put("targetPage", targetPage);
        item.put("payload", payload == null ? new LinkedHashMap<String, Object>() : payload);
        return item;
    }

    private void sortWorkbenchItems(List<Map<String, Object>> rows) {
        Collections.sort(rows, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> a, Map<String, Object> b) {
                LocalDateTime ta = (LocalDateTime) a.get("updatedAt");
                LocalDateTime tb = (LocalDateTime) b.get("updatedAt");
                if (ta == null && tb == null) return 0;
                if (ta == null) return 1;
                if (tb == null) return -1;
                return tb.compareTo(ta);
            }
        });
    }

    private boolean matchesOwnerFilter(String owner, String expected) {
        if (isBlank(expected)) return true;
        if (isBlank(owner)) return false;
        return owner.trim().equalsIgnoreCase(expected.trim());
    }

    private boolean matchScopedOwner(HttpServletRequest request, String owner) {
        return !isSalesScoped(request) || ownerMatchesScope(request, owner);
    }

    private boolean inRange(LocalDateTime value, LocalDateTime from, LocalDateTime to) {
        if (value == null) return true;
        if (from != null && value.isBefore(from)) return false;
        if (to != null && value.isAfter(to)) return false;
        return true;
    }
}
