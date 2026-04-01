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
import java.util.*;

@Configuration
public class DataInitializer {

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;
    @Value("${auth.bootstrap.default-password:CHANGE_ME_IN_PRODUCTION}")
    private String bootstrapDefaultPassword;
    @Value("${app.seed.tenant-id:}")
    private String seedTenantId;

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
            String tenantId = requireSeedTenantId();
            if (!tenantRepository.existsById(tenantId)) {
                Tenant tenant = new Tenant();
                tenant.setId(tenantId);
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
                customerRepository.save(makeCustomer("c_1001", "\u534e\u6676\u5236\u9020", "wang", "A", 320000L, "Active"));
            }
            if (!customerRepository.existsById("c_1002")) {
                customerRepository.save(makeCustomer("c_1002", "\u51cc\u950b\u533b\u7597", "zhao", "Renewal", 210000L, "Pending"));
            }
            if (!customerRepository.existsById("c_1003")) {
                customerRepository.save(makeCustomer("c_1003", "\u542f\u5149\u79d1\u6280", "chen", "New", 98000L, "Pending"));
            }
            if (!customerRepository.existsById("c_1004")) {
                customerRepository.save(makeCustomer("c_1004", "\u84dd\u56fe\u7269\u6d41", "li", "HighPotential", 450000L, "Active"));
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
                taskRepository.save(makeTask("t_3001", "\u51c6\u5907\u65b9\u6848\u8bc4\u5ba1\u4f1a", "today 15:30", "High", false, "manager"));
            }
            if (!taskRepository.existsById("t_3002")) {
                taskRepository.save(makeTask("t_3002", "\u786e\u8ba4\u91c7\u8d2d\u9884\u7b97", "today 17:00", "Medium", false, "sales"));
            }
            if (!taskRepository.existsById("t_3003")) {
                taskRepository.save(makeTask("t_3003", "\u5237\u65b0\u5b63\u5ea6\u9500\u552e\u5206\u6790", "tomorrow 10:00", "High", false, "admin"));
            }

            if (!followUpRepository.existsById("f_4001")) {
                followUpRepository.save(makeFollowUp("f_4001", "c_1001", "admin", "\u786e\u8ba4\u5408\u540c\u6700\u7ec8\u6761\u6b3e", "Phone", "Pending", null));
            }
            if (!followUpRepository.existsById("f_4002")) {
                followUpRepository.save(makeFollowUp("f_4002", "c_1002", "sales", "\u5df2\u53d1\u9001\u4fee\u8ba2\u62a5\u4ef7\u4e0e\u4e0a\u7ebf\u8ba1\u5212", "Email", "Waiting Feedback", null));
            }

            if (!contactRepository.existsById("ct_5001")) {
                contactRepository.save(makeContact("ct_5001", "c_1001", "\u5468\u51ef", "\u91c7\u8d2d\u603b\u76d1", "13800138001", "zhoukai@huajing.com", "wang"));
            }
            if (!contactRepository.existsById("ct_5002")) {
                contactRepository.save(makeContact("ct_5002", "c_1002", "\u5218\u654f", "IT\u7ecf\u7406", "13800138002", "liumin@lingfeng.com", "zhao"));
            }

            if (!contractRepository.existsById("cr_6001")) {
                contractRepository.save(makeContract("cr_6001", "c_1001", "CT-2026001", "\u6570\u5b57\u5316\u5347\u7ea7\u9879\u76ee", 180000L, "Draft", LocalDate.now().minusDays(12), "wang"));
            }
            if (!contractRepository.existsById("cr_6002")) {
                contractRepository.save(makeContract("cr_6002", "c_1002", "CT-2026002", "\u6570\u636e\u96c6\u6210", 260000L, "Signed", LocalDate.now().minusDays(25), "zhao"));
            }

            if (!paymentRepository.existsById("pm_7001")) {
                paymentRepository.save(makePayment("pm_7001", "c_1002", "cr_6002", 120000L, LocalDate.now().minusDays(5), "Bank", "Received", "\u4e00\u671f\u56de\u6b3e", "zhao"));
            }
            if (!paymentRepository.existsById("pm_7002")) {
                paymentRepository.save(makePayment("pm_7002", "c_1001", "cr_6001", 0L, null, "Bank", "Pending", "\u7b49\u5f85\u5408\u540c\u7b7e\u7f72\u540e\u56de\u6b3e", "wang"));
            }

            if (!productRepository.existsById("prd_8001")) {
                productRepository.save(makeProduct("prd_8001", "SKU-CRM-001", "CRM\u6388\u6743", "Software", 120000L, 0.06, "CNY"));
            }
            if (!productRepository.existsById("prd_8002")) {
                productRepository.save(makeProduct("prd_8002", "SKU-SRV-001", "\u5b9e\u65bd\u670d\u52a1", "Service", 80000L, 0.06, "CNY"));
            }

            if (!quoteRepository.existsById("qt_9001")) {
                quoteRepository.save(makeQuote("qt_9001", "QT-2026001", "c_1001", "o_2001", "wang", "APPROVED", 200000L, 12000L, 212000L));
            }
            if (!quoteItemRepository.existsById("qti_9001")) {
                quoteItemRepository.save(makeQuoteItem("qti_9001", "qt_9001", "prd_8001", "CRM\u6388\u6743", 1, 120000L, 0.0, 0.06, 120000L, 7200L, 127200L));
            }
            if (!quoteItemRepository.existsById("qti_9002")) {
                quoteItemRepository.save(makeQuoteItem("qti_9002", "qt_9001", "prd_8002", "\u5b9e\u65bd\u670d\u52a1", 1, 80000L, 0.0, 0.06, 80000L, 4800L, 84800L));
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

            upsertUser(userAccountRepository, passwordEncoder, "u_admin", "admin", bootstrapDefaultPassword, "ADMIN", "\u7cfb\u7edf\u7ba1\u7406\u5458", "");
            upsertUser(userAccountRepository, passwordEncoder, "u_manager", "manager", bootstrapDefaultPassword, "MANAGER", "\u9500\u552e\u7ecf\u7406", "");
            upsertUser(userAccountRepository, passwordEncoder, "u_sales", "sales", bootstrapDefaultPassword, "SALES", "\u9500\u552e", "sales");
            upsertUser(userAccountRepository, passwordEncoder, "u_analyst", "analyst", bootstrapDefaultPassword, "ANALYST", "\u6570\u636e\u5206\u6790\u5e08", "");

            backfillLegacySeedTexts(taskRepository, followUpRepository, contactRepository, contractRepository, paymentRepository, userAccountRepository);

            normalizeLegacyValues(customerRepository, opportunityRepository, contractRepository, paymentRepository,
                followUpRepository, contactRepository, taskRepository, auditLogRepository, valueNormalizerService);
        };
    }

    private String requireSeedTenantId() {
        if (seedTenantId == null || seedTenantId.trim().isEmpty()) {
            throw new IllegalStateException("seed_tenant_id_required");
        }
        return seedTenantId.trim();
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
        // Customer status normalization - batch save
        List<Customer> customersToUpdate = new ArrayList<>();
        for (Customer customer : customerRepository.findAll()) {
            String normalized = normalizer.normalizeCustomerStatus(customer.getStatus());
            if (!equalsSafe(customer.getStatus(), normalized)) {
                customer.setStatus(normalized);
                customersToUpdate.add(customer);
            }
        }
        if (!customersToUpdate.isEmpty()) {
            customerRepository.saveAll(customersToUpdate);
        }

        // Opportunity stage normalization - batch save
        List<Opportunity> opportunitiesToUpdate = new ArrayList<>();
        for (Opportunity opportunity : opportunityRepository.findAll()) {
            String normalized = normalizer.normalizeOpportunityStage(opportunity.getStage());
            if (!equalsSafe(opportunity.getStage(), normalized)) {
                opportunity.setStage(normalized);
                opportunitiesToUpdate.add(opportunity);
            }
        }
        if (!opportunitiesToUpdate.isEmpty()) {
            opportunityRepository.saveAll(opportunitiesToUpdate);
        }

        // ContractRecord status normalization - batch save
        List<ContractRecord> contractsToUpdate = new ArrayList<>();
        for (ContractRecord contract : contractRepository.findAll()) {
            String normalized = normalizer.normalizeContractStatus(contract.getStatus());
            if (!equalsSafe(contract.getStatus(), normalized)) {
                contract.setStatus(normalized);
                contractsToUpdate.add(contract);
            }
        }
        if (!contractsToUpdate.isEmpty()) {
            contractRepository.saveAll(contractsToUpdate);
        }

        // PaymentRecord status+method normalization - batch save
        List<PaymentRecord> paymentsToUpdate = new ArrayList<>();
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
                paymentsToUpdate.add(payment);
            }
        }
        if (!paymentsToUpdate.isEmpty()) {
            paymentRepository.saveAll(paymentsToUpdate);
        }

        // FollowUp channel normalization + tenantId fill - batch save
        List<FollowUp> followUpsToUpdate = new ArrayList<>();
        for (FollowUp followUp : followUpRepository.findAll()) {
            String normalizedChannel = normalizer.normalizeFollowUpChannel(followUp.getChannel());
            boolean changed = false;
            if (isBlank(followUp.getTenantId())) {
                followUp.setTenantId(seedTenantId);
                changed = true;
            }
            if (!equalsSafe(followUp.getChannel(), normalizedChannel)) {
                followUp.setChannel(normalizedChannel);
                changed = true;
            } else if (changed) {
                followUpsToUpdate.add(followUp);
            }
            if (changed) {
                followUpsToUpdate.add(followUp);
            }
        }
        if (!followUpsToUpdate.isEmpty()) {
            followUpRepository.saveAll(followUpsToUpdate);
        }

        // TenantId batch fills - batch save
        List<Customer> customerTenants = new ArrayList<>();
        for (Customer customer : customerRepository.findAll()) {
            if (isBlank(customer.getTenantId())) {
                customer.setTenantId(seedTenantId);
                customerTenants.add(customer);
            }
        }
        if (!customerTenants.isEmpty()) {
            customerRepository.saveAll(customerTenants);
        }

        List<Opportunity> oppTenants = new ArrayList<>();
        for (Opportunity opportunity : opportunityRepository.findAll()) {
            if (isBlank(opportunity.getTenantId())) {
                opportunity.setTenantId(seedTenantId);
                oppTenants.add(opportunity);
            }
        }
        if (!oppTenants.isEmpty()) {
            opportunityRepository.saveAll(oppTenants);
        }

        List<ContractRecord> contractTenants = new ArrayList<>();
        for (ContractRecord contract : contractRepository.findAll()) {
            if (isBlank(contract.getTenantId())) {
                contract.setTenantId(seedTenantId);
                contractTenants.add(contract);
            }
        }
        if (!contractTenants.isEmpty()) {
            contractRepository.saveAll(contractTenants);
        }

        List<PaymentRecord> paymentTenants = new ArrayList<>();
        for (PaymentRecord payment : paymentRepository.findAll()) {
            if (isBlank(payment.getTenantId())) {
                payment.setTenantId(seedTenantId);
                paymentTenants.add(payment);
            }
        }
        if (!paymentTenants.isEmpty()) {
            paymentRepository.saveAll(paymentTenants);
        }

        List<Contact> contactTenants = new ArrayList<>();
        for (Contact contact : contactRepository.findAll()) {
            if (isBlank(contact.getTenantId())) {
                contact.setTenantId(seedTenantId);
                contactTenants.add(contact);
            }
        }
        if (!contactTenants.isEmpty()) {
            contactRepository.saveAll(contactTenants);
        }

        List<TaskItem> taskTenants = new ArrayList<>();
        for (TaskItem task : taskRepository.findAll()) {
            if (isBlank(task.getTenantId())) {
                task.setTenantId(seedTenantId);
                taskTenants.add(task);
            }
        }
        if (!taskTenants.isEmpty()) {
            taskRepository.saveAll(taskTenants);
        }

        List<AuditLog> auditTenants = new ArrayList<>();
        for (AuditLog audit : auditLogRepository.findAll()) {
            if (isBlank(audit.getTenantId())) {
                audit.setTenantId(seedTenantId);
                auditTenants.add(audit);
            }
        }
        if (!auditTenants.isEmpty()) {
            auditLogRepository.saveAll(auditTenants);
        }
    }

    private boolean equalsSafe(String a, String b) {
        if (a == null) return b == null;
        return a.equals(b);
    }

    private void backfillLegacySeedTexts(TaskRepository taskRepository,
                                         FollowUpRepository followUpRepository,
                                         ContactRepository contactRepository,
                                         ContractRecordRepository contractRepository,
                                         PaymentRecordRepository paymentRepository,
                                         UserAccountRepository userAccountRepository) {
        rewriteTaskIfLegacy(taskRepository, "t_3001", "Prepare proposal review", "\u51c6\u5907\u65b9\u6848\u8bc4\u5ba1\u4f1a");
        rewriteTaskIfLegacy(taskRepository, "t_3002", "Confirm budget with purchaser", "\u786e\u8ba4\u91c7\u8d2d\u9884\u7b97");
        rewriteTaskIfLegacy(taskRepository, "t_3003", "Refresh quarterly sales analysis", "\u5237\u65b0\u5b63\u5ea6\u9500\u552e\u5206\u6790");

        rewriteFollowUpIfLegacy(followUpRepository, "f_4001", "Confirm final contract clauses", "\u786e\u8ba4\u5408\u540c\u6700\u7ec8\u6761\u6b3e");
        rewriteFollowUpIfLegacy(followUpRepository, "f_4002", "Sent revised quotation and rollout plan", "\u5df2\u53d1\u9001\u4fee\u8ba2\u62a5\u4ef7\u4e0e\u4e0a\u7ebf\u8ba1\u5212");

        rewriteContactIfLegacy(contactRepository, "ct_5001", "Zhou Kai", "\u5468\u51ef");
        rewriteContactIfLegacy(contactRepository, "ct_5002", "Liu Min", "\u5218\u654f");

        rewriteContractIfLegacy(contractRepository, "cr_6001", "Digital Upgrade Project", "\u6570\u5b57\u5316\u5347\u7ea7\u9879\u76ee");
        rewriteContractIfLegacy(contractRepository, "cr_6002", "Data Integration", "\u6570\u636e\u96c6\u6210");

        rewritePaymentRemarkIfLegacy(paymentRepository, "pm_7001", "Phase 1 payment", "\u4e00\u671f\u56de\u6b3e");
        rewritePaymentRemarkIfLegacy(paymentRepository, "pm_7002", "Pending after contract sign", "\u7b49\u5f85\u5408\u540c\u7b7e\u7f72\u540e\u56de\u6b3e");

        rewriteUserDisplayNameIfLegacy(userAccountRepository, "admin", "System Admin", "\u7cfb\u7edf\u7ba1\u7406\u5458");
        rewriteUserDisplayNameIfLegacy(userAccountRepository, "manager", "Sales Manager", "\u9500\u552e\u7ecf\u7406");
        rewriteUserDisplayNameIfLegacy(userAccountRepository, "sales", "Sales Rep", "\u9500\u552e");
        rewriteUserDisplayNameIfLegacy(userAccountRepository, "analyst", "Data Analyst", "\u6570\u636e\u5206\u6790\u5e08");
    }

    private void rewriteTaskIfLegacy(TaskRepository repository, String id, String legacyTitle, String newTitle) {
        repository.findById(id).ifPresent(task -> {
            if (!seedTenantId.equals(task.getTenantId())) return;
            if (!legacyTitle.equals(task.getTitle())) return;
            task.setTitle(newTitle);
            repository.save(task);
        });
    }

    private void rewriteFollowUpIfLegacy(FollowUpRepository repository, String id, String legacySummary, String newSummary) {
        repository.findById(id).ifPresent(row -> {
            if (!seedTenantId.equals(row.getTenantId())) return;
            if (!legacySummary.equals(row.getSummary())) return;
            row.setSummary(newSummary);
            repository.save(row);
        });
    }

    private void rewriteContactIfLegacy(ContactRepository repository, String id, String legacyName, String newName) {
        repository.findById(id).ifPresent(row -> {
            if (!seedTenantId.equals(row.getTenantId())) return;
            if (!legacyName.equals(row.getName())) return;
            row.setName(newName);
            repository.save(row);
        });
    }

    private void rewriteContractIfLegacy(ContractRecordRepository repository, String id, String legacyTitle, String newTitle) {
        repository.findById(id).ifPresent(row -> {
            if (!seedTenantId.equals(row.getTenantId())) return;
            if (!legacyTitle.equals(row.getTitle())) return;
            row.setTitle(newTitle);
            repository.save(row);
        });
    }

    private void rewritePaymentRemarkIfLegacy(PaymentRecordRepository repository, String id, String legacyRemark, String newRemark) {
        repository.findById(id).ifPresent(row -> {
            if (!seedTenantId.equals(row.getTenantId())) return;
            if (!legacyRemark.equals(row.getRemark())) return;
            row.setRemark(newRemark);
            repository.save(row);
        });
    }

    private void rewriteUserDisplayNameIfLegacy(UserAccountRepository repository, String username, String legacyName, String newName) {
        repository.findByUsernameAndTenantId(username, seedTenantId).ifPresent(row -> {
            if (!legacyName.equals(row.getDisplayName())) return;
            row.setDisplayName(newName);
            repository.save(row);
        });
    }

    private Customer makeCustomer(String id, String name, String owner, String tag, Long value, String status) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName(name);
        customer.setOwner(owner);
        customer.setTag(tag);
        customer.setValue(value);
        customer.setStatus(status);
        customer.setTenantId(seedTenantId);
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
        opportunity.setTenantId(seedTenantId);
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
        taskItem.setTenantId(seedTenantId);
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
        followUp.setTenantId(seedTenantId);
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
        contact.setTenantId(seedTenantId);
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
        contract.setTenantId(seedTenantId);
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
        payment.setTenantId(seedTenantId);
        return payment;
    }

    private Product makeProduct(String id, String code, String name, String category, Long standardPrice, Double taxRate, String currency) {
        Product product = new Product();
        product.setId(id);
        product.setTenantId(seedTenantId);
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
        quote.setTenantId(seedTenantId);
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
        quote.setNotes("\u6f14\u793a\u62a5\u4ef7");
        return quote;
    }

    private QuoteItem makeQuoteItem(String id, String quoteId, String productId, String productName, Integer quantity, Long unitPrice,
                                    Double discountRate, Double taxRate, Long subtotal, Long tax, Long total) {
        QuoteItem item = new QuoteItem();
        item.setId(id);
        item.setTenantId(seedTenantId);
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
        order.setTenantId(seedTenantId);
        order.setOrderNo(orderNo);
        order.setCustomerId(customerId);
        order.setOpportunityId(opportunityId);
        order.setQuoteId(quoteId);
        order.setOwner(owner);
        order.setStatus(status);
        order.setAmount(amount);
        order.setSignDate(LocalDate.now().minusDays(2));
        order.setNotes("\u6f14\u793a\u8ba2\u5355");
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
        user.setTenantId(seedTenantId);
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
        Optional<UserAccount> existing = repository.findByUsernameAndTenantId(username, seedTenantId);
        UserAccount user = existing.orElseGet(() -> makeUser(id, username, "", role, displayName, ownerScope));
        user.setId(id);
        user.setUsername(username);
        if (!existing.isPresent()) {
            user.setPassword(encoder.encode(rawPassword));
        }
        user.setRole(role);
        user.setDisplayName(displayName);
        user.setOwnerScope(ownerScope);
        user.setEnabled(true);
        user.setTenantId(seedTenantId);
        user.setDepartment("DEFAULT");
        user.setDataScope("SELF");
        repository.save(user);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

