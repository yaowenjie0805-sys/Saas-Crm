package com.yao.crm.config;

import com.yao.crm.entity.ApprovalTemplate;
import com.yao.crm.entity.Contact;
import com.yao.crm.entity.ContractRecord;
import com.yao.crm.entity.Customer;
import com.yao.crm.entity.FollowUp;
import com.yao.crm.entity.Lead;
import com.yao.crm.entity.LeadAssignmentRule;
import com.yao.crm.entity.LeadImportJob;
import com.yao.crm.entity.LeadImportJobChunk;
import com.yao.crm.entity.LeadImportJobItem;
import com.yao.crm.entity.Opportunity;
import com.yao.crm.entity.OrderRecord;
import com.yao.crm.entity.PaymentRecord;
import com.yao.crm.entity.Product;
import com.yao.crm.entity.Quote;
import com.yao.crm.entity.QuoteItem;
import com.yao.crm.entity.TaskItem;
import com.yao.crm.entity.Tenant;
import com.yao.crm.entity.UserAccount;
import com.yao.crm.repository.ApprovalTemplateRepository;
import com.yao.crm.repository.ContactRepository;
import com.yao.crm.repository.ContractRecordRepository;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.FollowUpRepository;
import com.yao.crm.repository.LeadAssignmentRuleRepository;
import com.yao.crm.repository.LeadImportJobChunkRepository;
import com.yao.crm.repository.LeadImportJobItemRepository;
import com.yao.crm.repository.LeadImportJobRepository;
import com.yao.crm.repository.LeadRepository;
import com.yao.crm.repository.OpportunityRepository;
import com.yao.crm.repository.OrderRecordRepository;
import com.yao.crm.repository.PaymentRecordRepository;
import com.yao.crm.repository.ProductRepository;
import com.yao.crm.repository.QuoteItemRepository;
import com.yao.crm.repository.QuoteRepository;
import com.yao.crm.repository.TaskRepository;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.repository.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Configuration
public class DualMarketDemoDataInitializer {

    @Bean
    @Order(200)
    public CommandLineRunner seedDualMarketDemoData(TenantRepository tenantRepository,
                                                    UserAccountRepository userAccountRepository,
                                                    CustomerRepository customerRepository,
                                                    OpportunityRepository opportunityRepository,
                                                    LeadRepository leadRepository,
                                                    ContactRepository contactRepository,
                                                    FollowUpRepository followUpRepository,
                                                    TaskRepository taskRepository,
                                                    ProductRepository productRepository,
                                                    QuoteRepository quoteRepository,
                                                    QuoteItemRepository quoteItemRepository,
                                                    OrderRecordRepository orderRecordRepository,
                                                    ContractRecordRepository contractRecordRepository,
                                                    PaymentRecordRepository paymentRecordRepository,
                                                    ApprovalTemplateRepository approvalTemplateRepository,
                                                    LeadAssignmentRuleRepository leadAssignmentRuleRepository,
                                                    LeadImportJobRepository leadImportJobRepository,
                                                    LeadImportJobChunkRepository leadImportJobChunkRepository,
                                                    LeadImportJobItemRepository leadImportJobItemRepository,
                                                    PasswordEncoder passwordEncoder) {
        return args -> {
            seedTenantDemo("tenant_cn_demo", "China Demo Tenant", "CN", "CNY", "Asia/Shanghai", "VAT_CN", "STRICT",
                    "[\"WECOM\",\"DINGTALK\"]", "CN", "STANDARD", "cn",
                    tenantRepository, userAccountRepository, customerRepository, opportunityRepository, leadRepository,
                    contactRepository, followUpRepository, taskRepository, productRepository, quoteRepository,
                    quoteItemRepository, orderRecordRepository, contractRecordRepository, paymentRecordRepository,
                    approvalTemplateRepository, leadAssignmentRuleRepository, leadImportJobRepository, leadImportJobChunkRepository,
                    leadImportJobItemRepository, passwordEncoder);

            seedTenantDemo("tenant_global_demo", "Global Demo Tenant", "GLOBAL", "USD", "UTC", "VAT_GLOBAL", "STAGE_GATE",
                    "[\"EMAIL\",\"SLACK\"]", "GLOBAL", "STRICT", "gl",
                    tenantRepository, userAccountRepository, customerRepository, opportunityRepository, leadRepository,
                    contactRepository, followUpRepository, taskRepository, productRepository, quoteRepository,
                    quoteItemRepository, orderRecordRepository, contractRecordRepository, paymentRecordRepository,
                    approvalTemplateRepository, leadAssignmentRuleRepository, leadImportJobRepository, leadImportJobChunkRepository,
                    leadImportJobItemRepository, passwordEncoder);
        };
    }

    private void seedTenantDemo(String tenantId,
                                String tenantName,
                                String marketProfile,
                                String currency,
                                String timezone,
                                String taxRule,
                                String approvalMode,
                                String channelsJson,
                                String dataResidency,
                                String maskLevel,
                                String idPrefix,
                                TenantRepository tenantRepository,
                                UserAccountRepository userAccountRepository,
                                CustomerRepository customerRepository,
                                OpportunityRepository opportunityRepository,
                                LeadRepository leadRepository,
                                ContactRepository contactRepository,
                                FollowUpRepository followUpRepository,
                                TaskRepository taskRepository,
                                ProductRepository productRepository,
                                QuoteRepository quoteRepository,
                                QuoteItemRepository quoteItemRepository,
                                OrderRecordRepository orderRecordRepository,
                                ContractRecordRepository contractRecordRepository,
                                PaymentRecordRepository paymentRecordRepository,
                                ApprovalTemplateRepository approvalTemplateRepository,
                                LeadAssignmentRuleRepository leadAssignmentRuleRepository,
                                LeadImportJobRepository leadImportJobRepository,
                                LeadImportJobChunkRepository leadImportJobChunkRepository,
                                LeadImportJobItemRepository leadImportJobItemRepository,
                                PasswordEncoder passwordEncoder) {
        upsertTenant(tenantRepository, tenantId, tenantName, marketProfile, currency, timezone, taxRule, approvalMode,
                channelsJson, dataResidency, maskLevel);

        upsertUser(userAccountRepository, passwordEncoder, "u_" + idPrefix + "_admin", idPrefix + "_admin", "admin123",
                "ADMIN", marketProfile + " Admin", "", tenantId);
        upsertUser(userAccountRepository, passwordEncoder, "u_" + idPrefix + "_manager", idPrefix + "_manager", "manager123",
                "MANAGER", marketProfile + " Manager", "", tenantId);
        upsertUser(userAccountRepository, passwordEncoder, "u_" + idPrefix + "_sales", idPrefix + "_sales", "sales123",
                "SALES", marketProfile + " Sales", "sales", tenantId);
        upsertUser(userAccountRepository, passwordEncoder, "u_" + idPrefix + "_analyst", idPrefix + "_analyst", "analyst123",
                "ANALYST", marketProfile + " Analyst", "", tenantId);

        String[] ownerPool = new String[]{"sales", "manager", "admin", "analyst"};
        String[] customerNames = "GLOBAL".equals(marketProfile)
                ? new String[]{"Northwind Labs", "Borealis Retail", "Apex Mobility", "Blue Orbit Media", "Globex Energy", "Summit AI"}
                : new String[]{"华京制造", "凌峰医疗", "启光科技", "蓝图物流", "星河教育", "远山零售"};

        for (int i = 0; i < customerNames.length; i++) {
            int idx = i + 1;
            String owner = ownerPool[i % ownerPool.length];
            String customerId = "c_" + idPrefix + "_" + pad2(idx);
            upsertCustomer(customerRepository, customerId, customerNames[i], owner, idx % 2 == 0 ? "A" : "NEW",
                    180000L + (idx * 42000L), idx % 2 == 0 ? "ACTIVE" : "PENDING", tenantId);
            upsertOpportunity(opportunityRepository, "o_" + idPrefix + "_" + pad2(idx),
                    idx % 3 == 0 ? "PROPOSAL" : (idx % 2 == 0 ? "QUALIFIED" : "LEAD"),
                    8 + idx, 240000L + (idx * 36000L), 30 + idx * 10, owner, tenantId, currency, marketProfile);
            upsertLead(leadRepository, "l_" + idPrefix + "_" + pad2(idx), customerNames[i] + " lead",
                    customerNames[i], "1380000" + (2100 + idx), idPrefix + idx + "@demo.crm",
                    idx % 2 == 0 ? "QUALIFIED" : "NEW", owner, "GLOBAL".equals(marketProfile) ? "Website" : "WeCom", tenantId);
        }

        upsertContact(contactRepository, "ct_" + idPrefix + "_01", "c_" + idPrefix + "_01",
                "GLOBAL".equals(marketProfile) ? "Alex Carter" : "周凯",
                "GLOBAL".equals(marketProfile) ? "Procurement Director" : "采购总监",
                "13800138001", idPrefix + "_contact_01@demo.crm", "sales", tenantId);
        upsertContact(contactRepository, "ct_" + idPrefix + "_02", "c_" + idPrefix + "_02",
                "GLOBAL".equals(marketProfile) ? "Mia Brooks" : "刘敏",
                "GLOBAL".equals(marketProfile) ? "IT Manager" : "信息化经理",
                "13800138002", idPrefix + "_contact_02@demo.crm", "manager", tenantId);
        upsertContact(contactRepository, "ct_" + idPrefix + "_03", "c_" + idPrefix + "_03",
                "GLOBAL".equals(marketProfile) ? "Noah Price" : "陈卓",
                "GLOBAL".equals(marketProfile) ? "Operations Lead" : "运营负责人",
                "13800138003", idPrefix + "_contact_03@demo.crm", "admin", tenantId);

        upsertFollowUp(followUpRepository, "f_" + idPrefix + "_01", "c_" + idPrefix + "_01", "sales",
                "GLOBAL".equals(marketProfile) ? "Reviewed proposal and integration scope" : "确认实施范围与交付节点",
                "GLOBAL".equals(marketProfile) ? "EMAIL" : "PHONE", "PENDING", LocalDate.now().plusDays(2), tenantId);
        upsertFollowUp(followUpRepository, "f_" + idPrefix + "_02", "c_" + idPrefix + "_02", "manager",
                "GLOBAL".equals(marketProfile) ? "Budget approval waiting customer signoff" : "预算审批待客户签字",
                "GLOBAL".equals(marketProfile) ? "MEETING" : "WECHAT", "WAITING", LocalDate.now().plusDays(4), tenantId);
        upsertFollowUp(followUpRepository, "f_" + idPrefix + "_03", "c_" + idPrefix + "_03", "admin",
                "GLOBAL".equals(marketProfile) ? "Delivery schedule aligned" : "交付计划已同步",
                "GLOBAL".equals(marketProfile) ? "CALL" : "PHONE", "DONE", null, tenantId);

        upsertTask(taskRepository, "t_" + idPrefix + "_01",
                "GLOBAL".equals(marketProfile) ? "Prepare renewal workshop" : "准备续约评审会",
                "today 15:00", "HIGH", false, "sales", tenantId);
        upsertTask(taskRepository, "t_" + idPrefix + "_02",
                "GLOBAL".equals(marketProfile) ? "Review quote tax model" : "复核报价税制口径",
                "tomorrow 11:00", "MEDIUM", false, "manager", tenantId);
        upsertTask(taskRepository, "t_" + idPrefix + "_03",
                "GLOBAL".equals(marketProfile) ? "Sync onboarding schedule" : "同步上线排期",
                "this week", "HIGH", true, "admin", tenantId);

        upsertProduct(productRepository, "prd_" + idPrefix + "_01", "SKU-" + idPrefix.toUpperCase() + "-CRM",
                "GLOBAL".equals(marketProfile) ? "CRM Subscription" : "CRM 授权", "SOFTWARE",
                "GLOBAL".equals(marketProfile) ? 260000L : 180000L, 0.06, currency, marketProfile, tenantId);
        upsertProduct(productRepository, "prd_" + idPrefix + "_02", "SKU-" + idPrefix.toUpperCase() + "-PS",
                "GLOBAL".equals(marketProfile) ? "Professional Service" : "实施服务", "SERVICE",
                "GLOBAL".equals(marketProfile) ? 140000L : 100000L, 0.06, currency, marketProfile, tenantId);

        upsertQuote(quoteRepository, "q_" + idPrefix + "_01", "QT-" + idPrefix.toUpperCase() + "-001",
                "c_" + idPrefix + "_01", "o_" + idPrefix + "_01", "sales",
                "GLOBAL".equals(marketProfile) ? "SUBMITTED" : "APPROVED",
                300000L, 18000L, 318000L, currency, marketProfile, tenantId);
        upsertQuoteItem(quoteItemRepository, "qi_" + idPrefix + "_01", "q_" + idPrefix + "_01",
                "prd_" + idPrefix + "_01", "GLOBAL".equals(marketProfile) ? "CRM Subscription" : "CRM 授权",
                1, "GLOBAL".equals(marketProfile) ? 260000L : 180000L, 0.0, 0.06, tenantId);
        upsertQuoteItem(quoteItemRepository, "qi_" + idPrefix + "_02", "q_" + idPrefix + "_01",
                "prd_" + idPrefix + "_02", "GLOBAL".equals(marketProfile) ? "Professional Service" : "实施服务",
                1, "GLOBAL".equals(marketProfile) ? 140000L : 120000L, 0.0, 0.06, tenantId);

        upsertOrder(orderRecordRepository, "ord_" + idPrefix + "_01", "ORD-" + idPrefix.toUpperCase() + "-001",
                "c_" + idPrefix + "_01", "o_" + idPrefix + "_01", "q_" + idPrefix + "_01", "sales",
                "FULFILLING", 318000L, currency, marketProfile, tenantId);

        upsertContract(contractRecordRepository, "cr_" + idPrefix + "_01", "c_" + idPrefix + "_01",
                "CT-" + idPrefix.toUpperCase() + "-001",
                "GLOBAL".equals(marketProfile) ? "Annual CRM Contract" : "CRM 年度主合同",
                318000L, "SIGNED", LocalDate.now().minusDays(8), "manager", currency, marketProfile, tenantId);

        upsertPayment(paymentRecordRepository, "pm_" + idPrefix + "_01", "c_" + idPrefix + "_01",
                "cr_" + idPrefix + "_01", "ord_" + idPrefix + "_01",
                "GLOBAL".equals(marketProfile) ? 120000L : 80000L,
                LocalDate.now().minusDays(2), "BANK_TRANSFER", "RECEIVED",
                "GLOBAL".equals(marketProfile) ? "Phase-1 payment received" : "一期回款已到账",
                "sales", currency, marketProfile, tenantId);

        upsertApprovalTemplate(approvalTemplateRepository, "apt_" + idPrefix + "_01",
                tenantId, "QUOTE", "GLOBAL".equals(marketProfile) ? "Quote approval flow" : "报价审批流",
                200000L, null, "SALES", "DEFAULT", "[\"MANAGER\",\"ADMIN\"]",
                "{\"nodes\":[{\"key\":\"n1\",\"role\":\"MANAGER\"},{\"key\":\"n2\",\"role\":\"ADMIN\"}]}", 1, "PUBLISHED", true);

        upsertLeadAssignmentRule(
                leadAssignmentRuleRepository,
                "lar_" + idPrefix + "_01",
                tenantId,
                "Demo round robin",
                true,
                "[{\"username\":\"sales\",\"weight\":2,\"enabled\":true},{\"username\":\"manager\",\"weight\":1,\"enabled\":true}]",
                0
        );
        upsertLeadImportDemoData(
                leadImportJobRepository,
                leadImportJobChunkRepository,
                leadImportJobItemRepository,
                tenantId,
                idPrefix
        );
    }

    private void upsertTenant(TenantRepository repository, String id, String name, String marketProfile, String currency,
                              String timezone, String taxRule, String approvalMode, String channelsJson,
                              String dataResidency, String maskLevel) {
        Tenant tenant = repository.findById(id).orElseGet(Tenant::new);
        tenant.setId(id);
        tenant.setName(name);
        tenant.setStatus("ACTIVE");
        tenant.setQuotaUsers(120);
        tenant.setTimezone(timezone);
        tenant.setCurrency(currency);
        tenant.setDateFormat("yyyy-MM-dd");
        tenant.setMarketProfile(marketProfile);
        tenant.setTaxRule(taxRule);
        tenant.setApprovalMode(approvalMode);
        tenant.setChannelsJson(channelsJson);
        tenant.setDataResidency(dataResidency);
        tenant.setMaskLevel(maskLevel);
        repository.save(tenant);
    }

    private void upsertUser(UserAccountRepository repository,
                            PasswordEncoder encoder,
                            String id,
                            String username,
                            String rawPassword,
                            String role,
                            String displayName,
                            String ownerScope,
                            String tenantId) {
        UserAccount user = repository.findByUsernameAndTenantId(username, tenantId).orElseGet(UserAccount::new);
        user.setId(id);
        user.setUsername(username);
        user.setPassword(encoder.encode(rawPassword));
        user.setRole(role);
        user.setDisplayName(displayName);
        user.setOwnerScope(ownerScope);
        user.setEnabled(true);
        user.setTenantId(tenantId);
        user.setDepartment("DEFAULT");
        user.setDataScope("SELF");
        repository.save(user);
    }

    private void upsertCustomer(CustomerRepository repository, String id, String name, String owner, String tag,
                                Long value, String status, String tenantId) {
        Customer row = repository.findById(id).orElseGet(Customer::new);
        row.setId(id);
        row.setName(name);
        row.setOwner(owner);
        row.setTag(tag);
        row.setValue(value);
        row.setStatus(status);
        row.setTenantId(tenantId);
        repository.save(row);
    }

    private void upsertOpportunity(OpportunityRepository repository, String id, String stage, Integer count, Long amount,
                                   Integer progress, String owner, String tenantId, String currency, String marketProfile) {
        Opportunity row = repository.findById(id).orElseGet(Opportunity::new);
        row.setId(id);
        row.setStage(stage);
        row.setCount(count);
        row.setAmount(amount);
        row.setProgress(progress);
        row.setOwner(owner);
        row.setTenantId(tenantId);
        row.setSettlementCurrency(currency);
        row.setExchangeRateSnapshot(exchangeRateSnapshot(marketProfile));
        row.setTaxDisplayMode("GLOBAL".equals(marketProfile) ? "TAX_EXCLUSIVE" : "TAX_INCLUSIVE");
        row.setComplianceTag("GLOBAL".equals(marketProfile) ? "GDPR_SAFE" : "CN_LOCAL");
        repository.save(row);
    }

    private void upsertLead(LeadRepository repository, String id, String name, String company, String phone, String email,
                            String status, String owner, String source, String tenantId) {
        Lead row = repository.findById(id).orElseGet(Lead::new);
        row.setId(id);
        row.setName(name);
        row.setCompany(company);
        row.setPhone(phone);
        row.setEmail(email);
        row.setStatus(status);
        row.setOwner(owner);
        row.setSource(source);
        row.setTenantId(tenantId);
        repository.save(row);
    }

    private void upsertContact(ContactRepository repository, String id, String customerId, String name, String title,
                               String phone, String email, String owner, String tenantId) {
        Contact row = repository.findById(id).orElseGet(Contact::new);
        row.setId(id);
        row.setCustomerId(customerId);
        row.setName(name);
        row.setTitle(title);
        row.setPhone(phone);
        row.setEmail(email);
        row.setOwner(owner);
        row.setTenantId(tenantId);
        repository.save(row);
    }

    private void upsertFollowUp(FollowUpRepository repository, String id, String customerId, String author, String summary,
                                String channel, String result, LocalDate nextActionDate, String tenantId) {
        FollowUp row = repository.findById(id).orElseGet(FollowUp::new);
        row.setId(id);
        row.setCustomerId(customerId);
        row.setAuthor(author);
        row.setSummary(summary);
        row.setChannel(channel);
        row.setResult(result);
        row.setNextActionDate(nextActionDate);
        row.setTenantId(tenantId);
        repository.save(row);
    }

    private void upsertTask(TaskRepository repository, String id, String title, String time, String level,
                            boolean done, String owner, String tenantId) {
        TaskItem row = repository.findById(id).orElseGet(TaskItem::new);
        row.setId(id);
        row.setTitle(title);
        row.setTime(time);
        row.setLevel(level);
        row.setDone(done);
        row.setOwner(owner);
        row.setTenantId(tenantId);
        repository.save(row);
    }

    private void upsertProduct(ProductRepository repository, String id, String code, String name, String category,
                               Long standardPrice, Double taxRate, String currency, String marketProfile, String tenantId) {
        Product row = repository.findById(id).orElseGet(Product::new);
        row.setId(id);
        row.setTenantId(tenantId);
        row.setCode(code);
        row.setName(name);
        row.setCategory(category);
        row.setStatus("ACTIVE");
        row.setStandardPrice(standardPrice);
        row.setTaxRate(taxRate);
        row.setCurrency(currency);
        row.setUnit("item");
        row.setSaleRegion(marketProfile);
        repository.save(row);
    }

    private void upsertQuote(QuoteRepository repository, String id, String quoteNo, String customerId, String opportunityId,
                             String owner, String status, Long subtotal, Long tax, Long total,
                             String currency, String marketProfile, String tenantId) {
        Quote row = repository.findById(id).orElseGet(Quote::new);
        row.setId(id);
        row.setTenantId(tenantId);
        row.setQuoteNo(quoteNo);
        row.setCustomerId(customerId);
        row.setOpportunityId(opportunityId);
        row.setOwner(owner);
        row.setStatus(status);
        row.setSubtotalAmount(subtotal);
        row.setTaxAmount(tax);
        row.setTotalAmount(total);
        row.setSettlementCurrency(currency);
        row.setExchangeRateSnapshot(exchangeRateSnapshot(marketProfile));
        row.setInvoiceStatus("GLOBAL".equals(marketProfile) ? "PENDING" : "NOT_REQUIRED");
        row.setTaxDisplayMode("GLOBAL".equals(marketProfile) ? "TAX_EXCLUSIVE" : "TAX_INCLUSIVE");
        row.setComplianceTag("GLOBAL".equals(marketProfile) ? "GDPR_SAFE" : "CN_LOCAL");
        row.setVersion(1);
        row.setValidUntil(LocalDate.now().plusDays(30));
        row.setNotes("GLOBAL".equals(marketProfile) ? "Demo quote for global stage-gate" : "演示报价（国内强审批）");
        repository.save(row);
    }

    private void upsertQuoteItem(QuoteItemRepository repository, String id, String quoteId, String productId,
                                 String productName, Integer quantity, Long unitPrice, Double discountRate, Double taxRate,
                                 String tenantId) {
        QuoteItem row = repository.findById(id).orElseGet(QuoteItem::new);
        long subtotal = unitPrice * quantity;
        long tax = Math.round(subtotal * taxRate);
        row.setId(id);
        row.setTenantId(tenantId);
        row.setQuoteId(quoteId);
        row.setProductId(productId);
        row.setProductName(productName);
        row.setQuantity(quantity);
        row.setUnitPrice(unitPrice);
        row.setDiscountRate(discountRate);
        row.setTaxRate(taxRate);
        row.setSubtotalAmount(subtotal);
        row.setTaxAmount(tax);
        row.setTotalAmount(subtotal + tax);
        repository.save(row);
    }

    private void upsertOrder(OrderRecordRepository repository, String id, String orderNo, String customerId, String opportunityId,
                             String quoteId, String owner, String status, Long amount,
                             String currency, String marketProfile, String tenantId) {
        OrderRecord row = repository.findById(id).orElseGet(OrderRecord::new);
        row.setId(id);
        row.setTenantId(tenantId);
        row.setOrderNo(orderNo);
        row.setCustomerId(customerId);
        row.setOpportunityId(opportunityId);
        row.setQuoteId(quoteId);
        row.setOwner(owner);
        row.setStatus(status);
        row.setAmount(amount);
        row.setSettlementCurrency(currency);
        row.setExchangeRateSnapshot(exchangeRateSnapshot(marketProfile));
        row.setInvoiceStatus("GLOBAL".equals(marketProfile) ? "PENDING" : "NOT_REQUIRED");
        row.setTaxDisplayMode("GLOBAL".equals(marketProfile) ? "TAX_EXCLUSIVE" : "TAX_INCLUSIVE");
        row.setComplianceTag("GLOBAL".equals(marketProfile) ? "GDPR_SAFE" : "CN_LOCAL");
        row.setSignDate(LocalDate.now().minusDays(3));
        row.setNotes("GLOBAL".equals(marketProfile) ? "Global demo order" : "国内演示订单");
        repository.save(row);
    }

    private void upsertContract(ContractRecordRepository repository, String id, String customerId, String contractNo,
                                String title, Long amount, String status, LocalDate signDate, String owner,
                                String currency, String marketProfile, String tenantId) {
        ContractRecord row = repository.findById(id).orElseGet(ContractRecord::new);
        row.setId(id);
        row.setCustomerId(customerId);
        row.setContractNo(contractNo);
        row.setTitle(title);
        row.setAmount(amount);
        row.setStatus(status);
        row.setSignDate(signDate);
        row.setOwner(owner);
        row.setTenantId(tenantId);
        row.setSettlementCurrency(currency);
        row.setExchangeRateSnapshot(exchangeRateSnapshot(marketProfile));
        row.setInvoiceStatus("GLOBAL".equals(marketProfile) ? "PENDING" : "NOT_REQUIRED");
        row.setTaxDisplayMode("GLOBAL".equals(marketProfile) ? "TAX_EXCLUSIVE" : "TAX_INCLUSIVE");
        row.setComplianceTag("GLOBAL".equals(marketProfile) ? "GDPR_SAFE" : "CN_LOCAL");
        repository.save(row);
    }

    private void upsertPayment(PaymentRecordRepository repository, String id, String customerId, String contractId,
                               String orderId, Long amount, LocalDate receivedDate, String method, String status,
                               String remark, String owner, String currency, String marketProfile, String tenantId) {
        PaymentRecord row = repository.findById(id).orElseGet(PaymentRecord::new);
        row.setId(id);
        row.setCustomerId(customerId);
        row.setContractId(contractId);
        row.setOrderId(orderId);
        row.setAmount(amount);
        row.setReceivedDate(receivedDate);
        row.setMethod(method);
        row.setStatus(status);
        row.setRemark(remark);
        row.setOwner(owner);
        row.setTenantId(tenantId);
        row.setSettlementCurrency(currency);
        row.setExchangeRateSnapshot(exchangeRateSnapshot(marketProfile));
        row.setInvoiceStatus("GLOBAL".equals(marketProfile) ? "PENDING" : "NOT_REQUIRED");
        row.setTaxDisplayMode("GLOBAL".equals(marketProfile) ? "TAX_EXCLUSIVE" : "TAX_INCLUSIVE");
        row.setComplianceTag("GLOBAL".equals(marketProfile) ? "GDPR_SAFE" : "CN_LOCAL");
        repository.save(row);
    }

    private void upsertApprovalTemplate(ApprovalTemplateRepository repository, String id, String tenantId, String bizType,
                                        String name, Long amountMin, Long amountMax, String role, String department,
                                        String approverRoles, String flowDefinition, Integer version, String status, Boolean enabled) {
        ApprovalTemplate row = repository.findById(id).orElseGet(ApprovalTemplate::new);
        row.setId(id);
        row.setTenantId(tenantId);
        row.setBizType(bizType);
        row.setName(name);
        row.setAmountMin(amountMin);
        row.setAmountMax(amountMax);
        row.setRole(role);
        row.setDepartment(department);
        row.setApproverRoles(approverRoles);
        row.setFlowDefinition(flowDefinition);
        row.setVersion(version);
        row.setStatus(status);
        row.setEnabled(enabled);
        repository.save(row);
    }

    private void upsertLeadAssignmentRule(LeadAssignmentRuleRepository repository,
                                          String id,
                                          String tenantId,
                                          String name,
                                          boolean enabled,
                                          String membersJson,
                                          int rrCursor) {
        LeadAssignmentRule row = repository.findById(id).orElseGet(LeadAssignmentRule::new);
        row.setId(id);
        row.setTenantId(tenantId);
        row.setName(name);
        row.setEnabled(enabled);
        row.setMembersJson(membersJson);
        row.setRrCursor(rrCursor);
        repository.save(row);
    }

    private void upsertLeadImportDemoData(LeadImportJobRepository jobRepository,
                                          LeadImportJobChunkRepository chunkRepository,
                                          LeadImportJobItemRepository itemRepository,
                                          String tenantId,
                                          String idPrefix) {
        String jobId = "lij_" + idPrefix + "_demo";
        LeadImportJob job = jobRepository.findById(jobId).orElseGet(LeadImportJob::new);
        job.setId(jobId);
        job.setTenantId(tenantId);
        job.setFileName("lead-import-" + idPrefix + ".csv");
        job.setStatus("PARTIAL_SUCCESS");
        job.setTotalRows(6);
        job.setProcessedRows(6);
        job.setSuccessCount(4);
        job.setFailCount(2);
        job.setPercent(100);
        job.setCreatedBy("admin");
        job.setCancelRequested(false);
        job.setErrorMessage("lead_import_partial_failure");
        job.setLastHeartbeatAt(LocalDateTime.now().minusMinutes(8));
        jobRepository.save(job);

        upsertLeadImportChunk(chunkRepository, "ljc_" + idPrefix + "_01", tenantId, jobId, 1, "PROCESSED", 0, null);
        upsertLeadImportChunk(chunkRepository, "ljc_" + idPrefix + "_02", tenantId, jobId, 2, "PROCESSED", 0, null);
        upsertLeadImportChunk(chunkRepository, "ljc_" + idPrefix + "_03", tenantId, jobId, 3, "PROCESSED", 0, null);

        upsertLeadImportItem(itemRepository, "lji_" + idPrefix + "_01", tenantId, jobId, 3,
                "FAILED", "Duplicate lead row", "lead_import_duplicate", "duplicate lead");
        upsertLeadImportItem(itemRepository, "lji_" + idPrefix + "_02", tenantId, jobId, 5,
                "FAILED", "Invalid phone", "contact_phone_invalid", "invalid phone");
    }

    private void upsertLeadImportChunk(LeadImportJobChunkRepository repository,
                                       String id,
                                       String tenantId,
                                       String jobId,
                                       int chunkNo,
                                       String status,
                                       int retryCount,
                                       String lastError) {
        LeadImportJobChunk row = repository.findById(id).orElseGet(LeadImportJobChunk::new);
        row.setId(id);
        row.setTenantId(tenantId);
        row.setJobId(jobId);
        row.setChunkNo(chunkNo);
        row.setStatus(status);
        row.setRetryCount(retryCount);
        row.setPayloadJson("[]");
        row.setLastError(lastError);
        repository.save(row);
    }

    private void upsertLeadImportItem(LeadImportJobItemRepository repository,
                                      String id,
                                      String tenantId,
                                      String jobId,
                                      int lineNo,
                                      String status,
                                      String rawLine,
                                      String errorCode,
                                      String errorMessage) {
        LeadImportJobItem row = repository.findById(id).orElseGet(LeadImportJobItem::new);
        row.setId(id);
        row.setTenantId(tenantId);
        row.setJobId(jobId);
        row.setLineNo(lineNo);
        row.setStatus(status);
        row.setRawLine(rawLine);
        row.setErrorCode(errorCode);
        row.setErrorMessage(errorMessage);
        row.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        repository.save(row);
    }

    private String exchangeRateSnapshot(String marketProfile) {
        return "GLOBAL".equals(marketProfile) ? "1.080000@ECB" : "1.000000@PBOC";
    }

    private String pad2(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }
}
