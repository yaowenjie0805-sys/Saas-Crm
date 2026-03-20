package com.yao.crm.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final DataSource dataSource;
    private final String rabbitHost;
    private final int rabbitPort;
    private final boolean mqEnabled;

    public HealthController(DataSource dataSource,
                            @Value("${RABBITMQ_HOST:127.0.0.1}") String rabbitHost,
                            @Value("${RABBITMQ_PORT:5672}") int rabbitPort,
                            @Value("${lead.import.mq.publish.enabled:true}") boolean mqEnabled) {
        this.dataSource = dataSource;
        this.rabbitHost = rabbitHost;
        this.rabbitPort = rabbitPort;
        this.mqEnabled = mqEnabled;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<String, Object>();
        Map<String, Object> readiness = ready();
        result.put("ok", Boolean.TRUE.equals(readiness.get("ok")));
        result.put("service", "crm-api-java");
        result.put("storage", "mysql");
        result.put("liveness", live());
        result.put("readiness", readiness);
        result.put("dependencies", dependencies());
        return result;
    }

    @GetMapping("/health/live")
    public Map<String, Object> live() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("ok", true);
        result.put("status", "UP");
        result.put("service", "crm-api-java");
        return result;
    }

    @GetMapping("/health/ready")
    public Map<String, Object> ready() {
        Map<String, Object> deps = dependencies();
        Map<String, Object> db = castMap(deps.get("database"));
        Map<String, Object> mq = castMap(deps.get("rabbitmq"));
        boolean dbOk = Boolean.TRUE.equals(db.get("ok"));
        boolean mqOk = Boolean.TRUE.equals(mq.get("ok")) || Boolean.TRUE.equals(mq.get("skipped"));
        boolean ok = dbOk && mqOk;

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("ok", ok);
        result.put("status", ok ? "UP" : "DEGRADED");
        result.put("service", "crm-api-java");
        result.put("checks", deps);
        return result;
    }

    @GetMapping("/health/deps")
    public Map<String, Object> dependencies() {
        Map<String, Object> deps = new HashMap<String, Object>();
        deps.put("database", databaseCheck());
        deps.put("rabbitmq", rabbitCheck());
        return deps;
    }

    private Map<String, Object> databaseCheck() {
        Map<String, Object> result = new HashMap<String, Object>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT 1")) {
            stmt.execute();
            result.put("ok", true);
            result.put("status", "UP");
        } catch (Exception ex) {
            result.put("ok", false);
            result.put("status", "DOWN");
            result.put("error", ex.getMessage());
        }
        return result;
    }

    private Map<String, Object> rabbitCheck() {
        Map<String, Object> result = new HashMap<String, Object>();
        if (!mqEnabled) {
            result.put("ok", true);
            result.put("skipped", true);
            result.put("status", "SKIPPED");
            result.put("reason", "lead.import.mq.publish.enabled=false");
            return result;
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(rabbitHost, rabbitPort), 1200);
            result.put("ok", true);
            result.put("status", "UP");
            result.put("host", rabbitHost);
            result.put("port", rabbitPort);
        } catch (Exception ex) {
            result.put("ok", false);
            result.put("status", "DOWN");
            result.put("host", rabbitHost);
            result.put("port", rabbitPort);
            result.put("error", ex.getMessage());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new HashMap<String, Object>();
    }
}
