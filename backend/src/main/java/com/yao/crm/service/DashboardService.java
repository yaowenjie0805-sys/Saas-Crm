package com.yao.crm.service;

import com.yao.crm.dto.DashboardResponse;
import com.yao.crm.dto.StatItem;
import com.yao.crm.entity.Customer;
import com.yao.crm.entity.Opportunity;
import com.yao.crm.entity.TaskItem;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.OpportunityRepository;
import com.yao.crm.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class DashboardService {

    private final CustomerRepository customerRepository;
    private final OpportunityRepository opportunityRepository;
    private final TaskRepository taskRepository;

    public DashboardService(CustomerRepository customerRepository, OpportunityRepository opportunityRepository, TaskRepository taskRepository) {
        this.customerRepository = customerRepository;
        this.opportunityRepository = opportunityRepository;
        this.taskRepository = taskRepository;
    }

    public DashboardResponse load() {
        List<Customer> customers = customerRepository.findAll();
        List<Opportunity> opportunities = opportunityRepository.findAll();
        List<TaskItem> tasks = taskRepository.findAll();

        long customerCount = customers.size();
        long totalSales = 0L;
        for (Customer customer : customers) {
            totalSales += customer.getValue() == null ? 0 : customer.getValue();
        }

        int avgCycle = 0;
        if (!opportunities.isEmpty()) {
            int total = 0;
            for (Opportunity opportunity : opportunities) {
                total += (100 - opportunity.getProgress());
            }
            avgCycle = Math.round((float) total / opportunities.size());
        }

        int doneTasks = 0;
        for (TaskItem task : tasks) {
            if (Boolean.TRUE.equals(task.getDone())) {
                doneTasks++;
            }
        }

        double retention = customerCount == 0 ? 0.0 : Math.round(((customerCount - doneTasks * 0.2) / customerCount) * 1000.0) / 10.0;

        DashboardResponse response = new DashboardResponse();
        response.setStats(Arrays.asList(
                new StatItem("Total Customers", String.valueOf(customerCount), "+6.3%"),
                new StatItem("Projected Revenue", formatMoney(totalSales), "+8.4%"),
                new StatItem("Avg Sales Cycle", avgCycle + " days", "-2.7%"),
                new StatItem("Retention Rate", retention + "%", "+1.1%")
        ));
        response.setOpportunities(opportunities);
        response.setTasks(tasks);
        response.setCustomers(customers);
        return response;
    }

    private String formatMoney(long value) {
        if (value >= 1000000) return "CNY " + String.format(Locale.US, "%.2f", (value / 1000000.0)) + "M";
        if (value >= 1000) return "CNY " + Math.round(value / 1000.0) + "K";
        return "CNY " + value;
    }
}