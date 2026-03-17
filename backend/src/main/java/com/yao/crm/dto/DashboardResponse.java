package com.yao.crm.dto;

import com.yao.crm.entity.Customer;
import com.yao.crm.entity.Opportunity;
import com.yao.crm.entity.TaskItem;

import java.util.List;

public class DashboardResponse {

    private List<StatItem> stats;
    private List<Opportunity> opportunities;
    private List<TaskItem> tasks;
    private List<Customer> customers;
    private String requestId;

    public List<StatItem> getStats() { return stats; }
    public void setStats(List<StatItem> stats) { this.stats = stats; }
    public List<Opportunity> getOpportunities() { return opportunities; }
    public void setOpportunities(List<Opportunity> opportunities) { this.opportunities = opportunities; }
    public List<TaskItem> getTasks() { return tasks; }
    public void setTasks(List<TaskItem> tasks) { this.tasks = tasks; }
    public List<Customer> getCustomers() { return customers; }
    public void setCustomers(List<Customer> customers) { this.customers = customers; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}
