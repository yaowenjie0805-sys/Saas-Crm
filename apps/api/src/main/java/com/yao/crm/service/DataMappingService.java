package com.yao.crm.service;

import com.yao.crm.entity.Contact;
import com.yao.crm.entity.Customer;
import com.yao.crm.entity.Lead;
import com.yao.crm.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 数据映射服务
 * 负责字段映射、类型转换、数据校验、模板生成等
 */
@Service
@Slf4j
public class DataMappingService {

    // 支持的实体类型
    public enum EntityType {
        CUSTOMER("Customer", "客户"),
        CONTACT("Contact", "联系人"),
        LEAD("Lead", "线索"),
        PRODUCT("Product", "产品"),
        OPPORTUNITY("Opportunity", "商机"),
        CONTRACT("Contract", "合同");

        private final String code;
        private final String name;

        EntityType(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getCode() { return code; }
        public String getName() { return name; }

        public static EntityType fromCode(String code) {
            for (EntityType type : values()) {
                if (type.code.equalsIgnoreCase(code)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown entity type: " + code);
        }
    }

    /**
     * 将行数据映射为实体
     */
    public Object mapRowToEntity(Map<String, String> row, List<String> headers,
                                  EntityType entityType, String tenantId) {
        switch (entityType) {
            case CUSTOMER:
                return mapToCustomer(row, tenantId);
            case CONTACT:
                return mapToContact(row, tenantId);
            case LEAD:
                return mapToLead(row, tenantId);
            case PRODUCT:
                return mapToProduct(row, tenantId);
            default:
                throw new IllegalArgumentException("Unsupported entity type for import: " + entityType);
        }
    }

    public Customer mapToCustomer(Map<String, String> row, String tenantId) {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID().toString());
        customer.setTenantId(tenantId);
        customer.setName(row.getOrDefault("客户名称", row.getOrDefault("name", "")));
        customer.setIndustry(row.getOrDefault("行业", row.getOrDefault("industry", "")));
        customer.setScale(row.getOrDefault("规模", row.getOrDefault("scale", "")));
        customer.setPhone(row.getOrDefault("电话", row.getOrDefault("phone", "")));
        customer.setWebsite(row.getOrDefault("网站", row.getOrDefault("website", "")));
        customer.setAddress(row.getOrDefault("地址", row.getOrDefault("address", "")));
        customer.setDescription(row.getOrDefault("描述", row.getOrDefault("description", "")));
        customer.setStatus(row.getOrDefault("状态", row.getOrDefault("status", "ACTIVE")));
        return customer;
    }

    public Contact mapToContact(Map<String, String> row, String tenantId) {
        Contact contact = new Contact();
        contact.setId(UUID.randomUUID().toString());
        contact.setTenantId(tenantId);
        contact.setName(row.getOrDefault("姓名", row.getOrDefault("name", "")));
        contact.setEmail(row.getOrDefault("邮箱", row.getOrDefault("email", "")));
        contact.setPhone(row.getOrDefault("电话", row.getOrDefault("phone", "")));
        contact.setMobile(row.getOrDefault("手机", row.getOrDefault("mobile", "")));
        contact.setPosition(row.getOrDefault("职位", row.getOrDefault("position", "")));
        contact.setCompany(row.getOrDefault("公司", row.getOrDefault("company", "")));
        return contact;
    }

    public Lead mapToLead(Map<String, String> row, String tenantId) {
        Lead lead = new Lead();
        lead.setId(UUID.randomUUID().toString());
        lead.setTenantId(tenantId);
        lead.setName(row.getOrDefault("姓名", row.getOrDefault("name", "")));
        lead.setCompany(row.getOrDefault("公司", row.getOrDefault("company", "")));
        lead.setPhone(row.getOrDefault("电话", row.getOrDefault("phone", "")));
        lead.setEmail(row.getOrDefault("邮箱", row.getOrDefault("email", "")));
        lead.setSource(row.getOrDefault("来源", row.getOrDefault("source", "")));
        lead.setStatus(row.getOrDefault("状态", row.getOrDefault("status", "NEW")));
        return lead;
    }

    public Product mapToProduct(Map<String, String> row, String tenantId) {
        Product product = new Product();
        product.setId(UUID.randomUUID().toString());
        product.setTenantId(tenantId);
        product.setName(row.getOrDefault("产品名称", row.getOrDefault("name", "")));
        product.setCode(row.getOrDefault("产品代码", row.getOrDefault("code", "")));
        product.setCategory(row.getOrDefault("分类", row.getOrDefault("category", "")));
        product.setUnit(row.getOrDefault("单位", row.getOrDefault("unit", "个")));
        product.setStatus(row.getOrDefault("状态", row.getOrDefault("status", "ACTIVE")));

        // 价格处理
        String priceStr = row.getOrDefault("价格", row.getOrDefault("price", "0"));
        try {
            product.setPrice(BigDecimal.valueOf(Double.parseDouble(priceStr)));
        } catch (NumberFormatException e) {
            product.setPrice(BigDecimal.ZERO);
        }

        return product;
    }

    /**
     * 获取模板表头
     */
    public List<String> getTemplateHeaders(EntityType type) {
        return getTemplateFields(type).stream()
                .map(this::getFieldDisplayName)
                .collect(Collectors.toList());
    }

    /**
     * 获取模板字段列表
     */
    public List<String> getTemplateFields(EntityType type) {
        switch (type) {
            case CUSTOMER:
                return Arrays.asList("name", "industry", "scale", "phone", "website", "address", "status");
            case CONTACT:
                return Arrays.asList("name", "email", "phone", "mobile", "position", "company");
            case LEAD:
                return Arrays.asList("name", "company", "phone", "email", "source", "status");
            case PRODUCT:
                return Arrays.asList("name", "code", "category", "unit", "price", "status");
            default:
                return new ArrayList<>();
        }
    }

    /**
     * 获取字段显示名称
     */
    public String getFieldDisplayName(String field) {
        Map<String, String> fieldNames = new HashMap<>();
        fieldNames.put("name", "名称");
        fieldNames.put("industry", "行业");
        fieldNames.put("scale", "规模");
        fieldNames.put("phone", "电话");
        fieldNames.put("website", "网站");
        fieldNames.put("address", "地址");
        fieldNames.put("status", "状态");
        fieldNames.put("email", "邮箱");
        fieldNames.put("mobile", "手机");
        fieldNames.put("position", "职位");
        fieldNames.put("company", "公司");
        fieldNames.put("source", "来源");
        fieldNames.put("code", "代码");
        fieldNames.put("category", "分类");
        fieldNames.put("unit", "单位");
        fieldNames.put("price", "价格");
        return fieldNames.getOrDefault(field, field);
    }

    /**
     * 获取示例数据
     */
    public List<Map<String, Object>> getSampleData(EntityType type) {
        List<Map<String, Object>> samples = new ArrayList<>();

        switch (type) {
            case CUSTOMER:
                Map<String, Object> customer = new LinkedHashMap<>();
                customer.put("name", "示例公司");
                customer.put("industry", "互联网");
                customer.put("scale", "100-500人");
                customer.put("phone", "010-12345678");
                customer.put("website", "www.example.com");
                customer.put("address", "北京市朝阳区");
                customer.put("status", "ACTIVE");
                samples.add(customer);
                break;

            case CONTACT:
                Map<String, Object> contact = new LinkedHashMap<>();
                contact.put("name", "张三");
                contact.put("email", "zhangsan@example.com");
                contact.put("phone", "010-12345678");
                contact.put("mobile", "13800138000");
                contact.put("position", "经理");
                contact.put("company", "示例公司");
                samples.add(contact);
                break;

            case LEAD:
                Map<String, Object> lead = new LinkedHashMap<>();
                lead.put("name", "李四");
                lead.put("company", "测试公司");
                lead.put("phone", "010-87654321");
                lead.put("email", "lisi@example.com");
                lead.put("source", "官网");
                lead.put("status", "NEW");
                samples.add(lead);
                break;

            case PRODUCT:
                Map<String, Object> product = new LinkedHashMap<>();
                product.put("name", "示例产品");
                product.put("code", "P001");
                product.put("category", "软件");
                product.put("unit", "套");
                product.put("price", "9999.00");
                product.put("status", "ACTIVE");
                samples.add(product);
                break;
        }

        return samples;
    }

    /**
     * 工具方法：将行数据转为字符串
     */
    public String rowToString(Map<String, String> row) {
        return row.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", "));
    }

    /**
     * 转义CSV值
     */
    public String escapeCsvValue(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
