package com.idigicloud.accountopening;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class BankAccountOpeningApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(BankAccountOpeningApplication.class, args);
        String port = context.getEnvironment().getProperty("local.server.port", "unknown");

        System.out.println("""
                ============================================================
                  iDigi Bank - Account Opening Service Started
                  Port   : %s
                  Swagger: http://localhost:%s/api/swagger-ui.html
                  CBS    : http://localhost:8081/cbs/swagger-ui.html
                  KYC    : http://localhost:5000 (Python service)
                ============================================================
                """.formatted(port, port));
    }
}