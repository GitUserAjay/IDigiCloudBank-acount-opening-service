package com.idigicloud.accountopening;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class BankAccountOpeningApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context =
                SpringApplication.run(BankAccountOpeningApplication.class, args);

        String port    = context.getEnvironment().getProperty("local.server.port", "unknown");
        String cbsUrl  = context.getEnvironment().getProperty("cbs.service.base-url",
                "http://localhost:8081/cbs");
        String kycUrl  = context.getEnvironment().getProperty("kyc.service.base-url",
                "http://localhost:5000");

        String selfUrl = System.getenv("RENDER_EXTERNAL_URL") != null
                ? System.getenv("RENDER_EXTERNAL_URL") + "/api"
                : "http://localhost:" + port + "/api";

        System.out.printf("""
                ============================================================
                  iDigi Bank - Account Opening Service Started
                  Internal Port : %s
                  API Base URL  : %s
                  Swagger       : %s/swagger-ui.html
                  CBS Service   : %s
                  KYC Service   : %s
                ============================================================
                %n""", port, selfUrl, selfUrl, cbsUrl, kycUrl);
    }
}