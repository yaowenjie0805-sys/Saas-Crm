package com.yao.crm.config;

import com.yao.crm.entity.AuditLog;
import com.yao.crm.entity.Contact;
import com.yao.crm.entity.ContractRecord;
import com.yao.crm.entity.Customer;
import com.yao.crm.entity.FollowUp;
import com.yao.crm.entity.Opportunity;
import com.yao.crm.entity.OrderRecord;
import com.yao.crm.entity.PaymentRecord;
import com.yao.crm.entity.Product;
import com.yao.crm.entity.Quote;
import com.yao.crm.entity.QuoteItem;
import com.yao.crm.entity.TaskItem;
import com.yao.crm.entity.Tenant;
import com.yao.crm.entity.UserAccount;
import com.yao.crm.repository.AuditLogRepository;
import com.yao.crm.repository.ContactRepository;
import com.yao.crm.repository.ContractRecordRepository;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.FollowUpRepository;
import com.yao.crm.repository.OpportunityRepository;
import com.yao.crm.repository.OrderRecordRepository;
import com.yao.crm.repository.PaymentRecordRepository;
import com.yao.crm.repository.ProductRepository;
import com.yao.crm.repository.QuoteItemRepository;
import com.yao.crm.repository.QuoteRepository;
import com.yao.crm.repository.TaskRepository;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.repository.UserAccountRepository;
import com.yao.crm.service.ValueNormalizerService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Configuration
public class DataInitializer {

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Bean
    public CommandLineRunner seedData(CustomerRepository customerRepository,
                                      OpportunityRepository opportunityRepository,
                                      TaskRepository taskRepository,
                                      FollowUpRepository followUpRepository,
                                      ContactRepository contactRepository,
                                      ContractRecordRepository contractRepository,
                                      PaymentRecordRepository paymentRepository,
                                      ProductRepository productRepository,
                                      QuoteRepository quoteRepository,
                                      QuoteItemRepository quoteItemRepository,
                                      OrderRecordRepository orderRecordRepository,
                                      TenantRepository tenantRepository,
                                      AuditLogRepository auditLogRepository,
                                      UserAccountRepository userAccountRepository,
                                      PasswordEncoder passwordEncoder,
                                      ValueNormalizerService valueNormalizerService) {
        return args -> {
            if (!seedEnabled) {
                return;
            }
            if (!tenantRepository.existsById("tenant_default")) {
                Tenant tenant = new Tenant();
                tenant.setId("tenant_default");
                tenant.setName("Default Tenant");
                tenant.setStatus("ACTIVE");
                tenant.setQuotaUsers(100);
                tenant.setTimezone("Asia/Shanghai");
                tenant.setCurrency("CNY");
                tenant.setDateFormat("yyyy-MM-dd");
                tenant.setMarketProfile("CN");
                tenant.setTaxRule("VAT_CN");
                tenant.setApprovalMode("STRICT");
                tenant.setChannelsJson("[\"WECOM\",\"DINGTALK\"]");
                tenant.setDataResidency("CN");
                tenant.setMaskLevel("STANDARD");
                tenantRepository.save(tenant);
            }

            if (!customerRepository.existsById("c_1001")) {
                customerRepository.save(makeCustomer("c_1001", "HuaJing Manufacturing", "wang", "A", 320000L, "Active"));
            }
            if (!customerRepository.existsById("c_1002")) {
                customerRepository.save(makeCustomer("c_1002", "LingFeng Medical", "zhao", "Renewal", 210000L, "Pending"));
            }
            if (!customerRepository.existsById("c_1003")) {
                customerRepository.save(makeCustomer("c_1003", "QiGuang Tech", "chen", "New", 98000L, "Pending"));
            }
            if (!customerRepository.existsById("c_1004")) {
                customerRepository.save(makeCustomer("c_1004", "BlueMap Logistics", "li", "HighPotential", 450000L, "Active"));
            }

            if (!opportunityRepository.existsById("o_2001")) {
                opportunityRepository.save(makeOpportunity("o_2001", "Lead", 76, 420000L, 80, "chen"));
            }
            if (!opportunityRepository.existsById("o_2002")) {
                opportunityRepository.save(makeOpportunity("o_2002", "Qualified", 54, 360000L, 64, "zhao"));
            }
            if (!opportunityRepository.existsById("o_2003")) {
                opportunityRepository.save(makeOpportunity("o_2003", "Proposal", 31, 530000L, 48, "chen"));
            }
            if (!opportunityRepository.existsById("o_2004")) {
                opportunityRepository.save(makeOpportunity("o_2004", "Negotiation", 15, 290000L, 28, "li"));
            }
            if (!opportunityRepository.existsById("o_2005")) {
                opportunityRepository.save(makeOpportunity("o_2005", "Closed Won", 9, 260000L, 20, "chen"));
            }

            if (!taskRepository.existsById("t_3001")) {
                taskRepository.save(makeTask("t_3001", "Prepare proposal review", "today 15:30", "High", false, "manager"));
            }
            if (!taskRepository.existsById("t_3002")) {
                taskRepository.save(makeTask("t_3002", "Confirm budget with purchaser", "today 17:00", "Medium", false, "sales"));
            }
            if (!taskRepository.existsById("t_3003")) {
                taskRepository.save(makeTask("t_3003", "Refresh quarterly sales analysis", "tomorrow 10:00", "High", false, "admin"));
            }

            if (!followUpRepository.existsById("f_4001")) {
                followUpRepository.save(makeFollowUp("f_4001", "c_1001", "admin", "Confirm final contract clauses", "Phone", "Pending", null));
            }
            if (!followUpRepository.existsById("f_4002")) {
                followUpRepository.save(makeFollowUp("f_4002", "c_1002", "sales", "Sent revised quotation and rollout plan", "Email", "Waiting Feedback", null));
            }

            if (!contactRepository.existsById("ct_5001")) {
                contactRepository.save(makeContact("ct_5001", "c_1001", "Zhou Kai", "Procurement Director", "13800138001", "zhoukai@huajing.com", "wang"));
            }
            if (!contactRepository.existsById("ct_5002")) {
                contactRepository.save(makeContact("ct_5002", "c_1002", "Liu Min", "IT Manager", "13800138002", "liumin@lingfeng.com", "zhao"));
            }

            if (!contractRepository.existsById("cr_6001")) {
                contractRepository.save(makeContract("cr_6001", "c_1001", "CT-2026001", "Digital Upgrade Project", 180000L, "Draft", LocalDate.now().minusDays(12), "wang"));
            }
            if (!contractRepository.existsById("cr_6002")) {
                contractRepository.save(makeContract("cr_6002", "c_1002", "CT-2026002", "Data Integration", 260000L, "Signed", LocalDate.now().minusDays(25), "zhao"));
            }

            if (!paymentRepository.existsById("pm_7001")) {
                paymentRepository.save(makePayment("pm_7001", "c_1002", "cr_6002", 120000L, LocalDate.now().minusDays(5), "Bank", "Received", "Phase 1 payment", "zhao"));
            }
            if (!paymentRepository.existsById("pm_7002")) {
                paymentRepository.save(makePayment("pm_7002", "c_1001", "cr_6001", 0L, null, "Bank", "Pending", "Pending after contract sign", "wang"));
            }

            if (!productRepository.existsById("prd_8001")) {
                productRepository.save(makeProduct("prd_8001", "SKU-CRM-001", "CRM License", "Software", 120000L, 0.06, "CNY"));
            }
            if (!productRepository.existsById("prd_8002")) {
                productRepository.save(makeProduct("prd_8002", "SKU-SRV-001", "Implementation Service", "Service", 80000L, 0.06, "CNY"));
            }

            if (!quoteRepository.existsById("qt_9001")) {
                quoteRepository.save(makeQuote("qt_9001", "QT-2026001", "c_1001", "o_2001", "wang", "APPROVED", 200000L, 12000L, 212000L));
            }
            if (!quoteItemRepository.existsById("qti_9001")) {
                quoteItemRepository.save(makeQuoteItem("qti_9001", "qt_9001", "prd_8001", "CRM License", 1, 120000L, 0.0, 0.06, 120000L, 7200L, 127200L));
            }
            if (!quoteItemRepository.existsById("qti_9002")) {
                quoteItemRepository.save(makeQuoteItem("qti_9002", "qt_9001", "prd_8002", "Implementation Service", 1, 80000L, 0.0, 0.06, 80000L, 4800L, 84800L));
            }
            if (!orderRecordRepository.existsById("ord_9101")) {
                orderRecordRepository.save(makeOrder("ord_9101", "ORD-2026001", "c_1001", "o_2001", "qt_9001", "wang", "FULFILLING", 212000L));
            }

            paymentRepository.findById("pm_7001").ifPresent(payment -> {
                if (isBlank(payment.getOrderId())) {
                    payment.setOrderId("ord_9101");
                    paymentRepository.save(payment);
                }
            });

            upsertUser(userAccountRepository, passwordEncoder, "u_admin", "admin", "admin123", "ADMIN", "System Admin", "");
            upsertUser(userAccountRepository, passwordEncoder, "u_manager", "manager", "manager123", "MANAGER", "Sales Manager", "");
            upsertUser(userAccountRepository, passwordEncoder, "u_sales", "sales", "sales123", "SALES", "Sales Rep", "sales");
            upsertUser(userAccountRepository, passwordEncoder, "u_analyst", "analyst", "analyst123", "ANALYST", "Data Analyst", "");

            normalizeLegacyValues(customerRepository, opportunityRepository, contractRepository, paymentRepository,
                    followUpRepository, contactRepository, taskRepository, auditLogRepository, valueNormalizerService);
        };
    }

    private void normalizeLegacyValues(CustomerRepository customerRepository,
                                       OpportunityRepository opportunityRepository,
                                       ContractRecordRepository contractRepository,
                                       PaymentRecordRepository paymentRepository,
                                       FollowUpRepository followUpRepository,
                                       ContactRepository contactRepository,
                                       TaskRepository taskRepository,
                                       AuditLogRepository auditLogRepository,
                                       ValueNormalizerService normalizer) {
        for (Customer customer : customerRepository.findAll()) {
            String normalized = normalizer.normalizeCustomerStatus(customer.getStatus());
            if (!equalsSafe(customer.getStatus(), normalized)) {
                customer.setStatus(normalized);
                customerRepository.save(customer);
            }
        }

        for (Opportunity opportunity : opportunityRepository.findAll()) {
            String normalized = normalizer.normalizeOpportunityStage(opportunity.getStage());
            if (!equalsSafe(opportunity.getStage(), normalized)) {
                opportunity.setStage(normalized);
                opportunityRepository.save(opportunity);
            }
        }

        for (ContractRecord contract : contractRepository.findAll()) {
            String normalized = normalizer.normalizeContractStatus(contract.getStatus());
            if (!equalsSafe(contract.getStatus(), normalized)) {
                contract.setStatus(normalized);
                contractRepository.save(contract);
            }
        }

        for (PaymentRecord payment : paymentRepository.findAll()) {
            String normalizedStatus = normalizer.normalizePaymentStatus(payment.getStatus());
            String normalizedMethod = normalizer.normalizePaymentMethod(payment.getMethod());
            boolean changed = false;
            if (!equalsSafe(payment.getStatus(), normalizedStatus)) {
                payment.setStatus(normalizedStatus);
                changed = true;
            }
            if (!equalsSafe(payment.getMethod(), normalizedMethod)) {
                payment.setMethod(normalizedMethod);
                changed = true;
            }
            if (changed) {
                paymentRepository.save(payment);
            }
        }

        for (FollowUp followUp : followUpRepository.findAll()) {
            String normalizedChannel = normalizer.normalizeFollowUpChannel(followUp.getChannel());
            if (isBlank(followUp.getTenantId())) {
                followUp.setTenantId("tenant_default");
            }
            if (!equalsSafe(followUp.getChannel(), normalizedChannel)) {
                followUp.setChannel(normalizedChannel);
                followUpRepository.save(followUp);
            } else if (isBlank(followUp.getTenantId())) {
                followUpRepository.save(followUp);
            }
        }

        for (Customer customer : customerRepository.findAll()) {
            if (isBlank(customer.getTenantId())) {
                customer.setTenantId("tenant_default");
                customerRepository.save(customer);
            }
        }
        for (Opportunity opportunity : opportunityRepository.findAll()) {
            if (isBlank(opportunity.getTenantId())) {
                opportunity.setTenantId("tenant_default");
                opportunityRepository.save(opportunity);
            }
        }
        for (ContractRecord contract : contractRepository.findAll()) {
            if (isBlank(contract.getTenantId())) {
                contract.setTenantId("tenant_default");
                contractRepository.save(contract);
            }
        }
        for (PaymentRecord payment : paymentRepository.findAll()) {
            if (isBlank(payment.getTenantId())) {
                payment.setTenantId("tenant_default");
                paymentRepository.save(payment);
            }
        }
        for (Contact contact : contactRepository.findAll()) {
            if (isBlank(contact.getTenantId())) {
                contact.setTenantId("tenant_default");
                contactRepository.save(contact);
            }
        }
        for (TaskItem task : taskRepository.findAll()) {
            if (isBlank(task.getTenantId())) {
                task.setTenantId("tenant_default");
                taskRepository.save(task);
            }
        }
        for (AuditLog audit : auditLogRepository.findAll()) {
            if (isBlank(audit.getTenantId())) {
                audit.setTenantId("tenant_default");
                auditLogRepository.save(audit);
            }
        }
    }

    private boolean equalsSafe(String a, String b) {
        if (a == null) return b == null;
        return a.equals(b);
    }

    private Customer makeCustomer(String id, String name, String owner, String tag, Long value, String status) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName(name);
        customer.setOwner(owner);
        customer.setTag(tag);
        customer.setValue(value);
        customer.setStatus(status);
        customer.setTenantId("tenant_default");
        return customer;
    }

    private Opportunity makeOpportunity(String id, String stage, Integer count, Long amount, Integer progress, String owner) {
        Opportunity opportunity = new Opportunity();
        opportunity.setId(id);
        opportunity.setStage(stage);
        opportunity.setCount(count);
        opportunity.setAmount(amount);
        opportunity.setProgress(progress);
        opportunity.setOwner(owner);
        opportunity.setTenantId("tenant_default");
        return opportunity;
    }

    private TaskItem makeTask(String id, String title, String time, String level, Boolean done, String owner) {
        TaskItem taskItem = new TaskItem();
        taskItem.setId(id);
        taskItem.setTitle(title);
        taskItem.setTime(time);
        taskItem.setLevel(level);
        taskItem.setDone(done);
        taskItem.setOwner(owner);
        taskItem.setTenantId("tenant_default");
        return taskItem;
    }

    private FollowUp makeFollowUp(String id, String customerId, String author, String summary, String channel, String result, LocalDate nextActionDate) {
        FollowUp followUp = new FollowUp();
        followUp.setId(id);
        followUp.setCustomerId(customerId);
        followUp.setAuthor(author);
        followUp.setSummary(summary);
        followUp.setChannel(channel);
        followUp.setResult(result);
        followUp.setNextActionDate(nextActionDate);
        followUp.setTenantId("tenant_default");
        return followUp;
    }

    private Contact makeContact(String id, String customerId, String name, String title, String phone, String email, String owner) {
        Contact contact = new Contact();
        contact.setId(id);
        contact.setCustomerId(customerId);
        contact.setName(name);
        contact.setTitle(title);
        contact.setPhone(phone);
        contact.setEmail(email);
        contact.setOwner(owner);
        contact.setTenantId("tenant_default");
        return contact;
    }

    private ContractRecord makeContract(String id, String customerId, String contractNo, String title, Long amount, String status, LocalDate signDate, String owner) {
        ContractRecord contract = new ContractRecord();
        contract.setId(id);
        contract.setCustomerId(customerId);
        contract.setContractNo(contractNo);
        contract.setTitle(title);
        contract.setAmount(amount);
        contract.setStatus(status);
        contract.setSignDate(signDate);
        contract.setOwner(owner);
        contract.setTenantId("tenant_default");
        return contract;
    }

    private PaymentRecord makePayment(String id, String customerId, String contractId, Long amount, LocalDate receivedDate, String method, String status, String remark, String owner) {
        PaymentRecord payment = new PaymentRecord();
        payment.setId(id);
        payment.setCustomerId(customerId);
        payment.setContractId(contractId);
        payment.setAmount(amount);
        payment.setReceivedDate(receivedDate);
        payment.setMethod(method);
        payment.setStatus(status);
        payment.setRemark(remark);
        payment.setOwner(owner);
        payment.setTenantId("tenant_default");
        return payment;
    }

    private Product makeProduct(String id, String code, String name, String category, Long standardPrice, Double taxRate, String currency) {
        Product product = new Product();
        product.setId(id);
        product.setTenantId("tenant_default");
        product.setCode(code);
        product.setName(name);
        product.setCategory(category);
        product.setStatus("ACTIVE");
        product.setStandardPrice(standardPrice);
        product.setTaxRate(taxRate);
        product.setCurrency(currency);
        product.setUnit("item");
        product.setSaleRegion("CN");
        return product;
    }

    private Quote makeQuote(String id, String quoteNo, String customerId, String opportunityId, String owner, String status, Long subtotal, Long tax, Long total) {
        Quote quote = new Quote();
        quote.setId(id);
        quote.setTenantId("tenant_default");
        quote.setQuoteNo(quoteNo);
        quote.setCustomerId(customerId);
        quote.setOpportunityId(opportunityId);
        quote.setOwner(owner);
        quote.setStatus(status);
        quote.setSubtotalAmount(subtotal);
        quote.setTaxAmount(tax);
        quote.setTotalAmount(total);
        quote.setVersion(1);
        quote.setValidUntil(LocalDate.now().plusDays(30));
        quote.setNotes("Seed quote");
        return quote;
    }

    private QuoteItem makeQuoteItem(String id, String quoteId, String productId, String productName, Integer quantity, Long unitPrice,
                                    Double discountRate, Double taxRate, Long subtotal, Long tax, Long total) {
        QuoteItem item = new QuoteItem();
        item.setId(id);
        item.setTenantId("tenant_default");
        item.setQuoteId(quoteId);
        item.setProductId(productId);
        item.setProductName(productName);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setDiscountRate(discountRate);
        item.setTaxRate(taxRate);
        item.setSubtotalAmount(subtotal);
        item.setTaxAmount(tax);
        item.setTotalAmount(total);
        return item;
    }

    private OrderRecord makeOrder(String id, String orderNo, String customerId, String opportunityId, String quoteId, String owner, String status, Long amount) {
        OrderRecord order = new OrderRecord();
        order.setId(id);
        order.setTenantId("tenant_default");
        order.setOrderNo(orderNo);
        order.setCustomerId(customerId);
        order.setOpportunityId(opportunityId);
        order.setQuoteId(quoteId);
        order.setOwner(owner);
        order.setStatus(status);
        order.setAmount(amount);
        order.setSignDate(LocalDate.now().minusDays(2));
        order.setNotes("Seed order");
        return order;
    }

    private UserAccount makeUser(String id, String username, String password, String role, String displayName, String ownerScope) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        user.setDisplayName(displayName);
        user.setOwnerScope(ownerScope);
        user.setEnabled(true);
        user.setTenantId("tenant_default");
        user.setDepartment("DEFAULT");
        user.setDataScope("SELF");
        return user;
    }

    private void upsertUser(UserAccountRepository repository,
                            PasswordEncoder encoder,
                            String id,
                            String username,
                            String rawPassword,
                            String role,
                            String displayName,
                            String ownerScope) {
        UserAccount user = repository.findByUsernameAndTenantId(username, "tenant_default")
                .orElseGet(() -> makeUser(id, username, "", role, displayName, ownerScope));
        user.setId(id);
        user.setUsername(username);
        user.setPassword(encoder.encode(rawPassword));
        user.setRole(role);
        user.setDisplayName(displayName);
        user.setOwnerScope(ownerScope);
        user.setEnabled(true);
        user.setTenantId("tenant_default");
        user.setDepartment("DEFAULT");
        user.setDataScope("SELF");
        repository.save(user);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
