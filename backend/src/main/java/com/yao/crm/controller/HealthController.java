package com.yao.crm.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("ok", true);
        result.put("service", "crm-api-java");
        result.put("storage", "mysql");
        return result;
    }
}