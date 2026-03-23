package com.yao.crm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.yao.crm.repository")
@EnableScheduling
public class CrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrmApplication.class, args);
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public ApplicationRunner initOnlyExitRunner(ApplicationContext applicationContext,
                                                @Value("${app.init-only:false}") boolean initOnly) {
        return args -> {
            if (!initOnly) {
                return;
            }
            int code = SpringApplication.exit(applicationContext, () -> 0);
            System.exit(code);
        };
    }
}
