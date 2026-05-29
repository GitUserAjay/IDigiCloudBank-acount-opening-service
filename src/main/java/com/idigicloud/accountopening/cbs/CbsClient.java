package com.idigicloud.accountopening.cbs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * HTTP client for communicating with the CBS Mock Service.
 * All CBS interactions go through this class.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CbsClient {

    @Value("${cbs.service.base-url}")
    private String cbsBaseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // ─────────────────────────────────────────
    //  CUSTOMER
    // ─────────────────────────────────────────

    public JsonNode createCustomer(Map<String, Object> payload) {
        return post("/api/v1/customers", payload);
    }

    public JsonNode getCustomerById(String customerId) {
        return get("/api/v1/customers/" + customerId);
    }

    public JsonNode searchCustomer(String searchBy, String searchValue) {
        return get("/api/v1/customers/search?searchBy=" + searchBy + "&searchValue=" + searchValue);
    }

    // ─────────────────────────────────────────
    //  ACCOUNT
    // ─────────────────────────────────────────

    public JsonNode openAccount(Map<String, Object> payload) {
        return post("/api/v1/accounts", payload);
    }

    public JsonNode getAccountByNumber(String accountNumber) {
        return get("/api/v1/accounts/" + accountNumber);
    }

    public JsonNode getAccountsByCustomerId(String customerId) {
        return get("/api/v1/accounts/customer/" + customerId);
    }

    public JsonNode activateAccount(String accountNumber) {
        // POST is used because RestTemplate PATCH is unreliable on some JVM/runtime setups.
        return post("/api/v1/accounts/" + accountNumber + "/activate", null);
    }

    public JsonNode updateInitialFunding(Map<String, Object> payload) {
        return post("/api/v1/accounts/funding", payload);
    }

    public JsonNode getTransactionLimits(String accountNumber) {
        return get("/api/v1/accounts/" + accountNumber + "/transaction-limits");
    }

    public JsonNode issueWelcomeKit(String accountNumber) {
        return post("/api/v1/accounts/" + accountNumber + "/welcome-kit", null);
    }

    // ─────────────────────────────────────────
    //  NOMINEE
    // ─────────────────────────────────────────

    public JsonNode addNominee(Map<String, Object> payload) {
        return post("/api/v1/nominees", payload);
    }

    public JsonNode getNomineesByAccount(String accountNumber) {
        return get("/api/v1/nominees/" + accountNumber);
    }

    // ─────────────────────────────────────────
    //  PRIVATE HTTP HELPERS
    // ─────────────────────────────────────────

    private JsonNode get(String path) {
        try {
            log.debug("CBS GET: {}{}", cbsBaseUrl, path);
            ResponseEntity<String> response = restTemplate.getForEntity(cbsBaseUrl + path, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (HttpClientErrorException e) {
            log.error("CBS GET error [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return parseSafe(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("CBS GET unexpected error: {}", e.getMessage());
            throw new CbsServiceException("CBS service unavailable. Please ensure CBS Mock is running on port 8081.");
        }
    }

    private JsonNode post(String path, Object payload) {
        try {
            log.debug("CBS POST: {}{}", cbsBaseUrl, path);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(cbsBaseUrl + path, entity, String.class);
            return requireSuccessResponse(objectMapper.readTree(response.getBody()));
        } catch (HttpClientErrorException e) {
            log.error("CBS POST error [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw toCbsServiceException(e);
        } catch (CbsServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("CBS POST unexpected error: {}", e.getMessage());
            throw new CbsServiceException("CBS service unavailable. Please ensure CBS Mock is running on port 8081.");
        }
    }

    private JsonNode patch(String path, Object payload) {
        try {
            log.debug("CBS PATCH: {}{}", cbsBaseUrl, path);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    cbsBaseUrl + path, HttpMethod.PATCH, entity, String.class);
            return requireSuccessResponse(objectMapper.readTree(response.getBody()));
        } catch (HttpClientErrorException e) {
            log.error("CBS PATCH error [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw toCbsServiceException(e);
        } catch (CbsServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("CBS PATCH unexpected error: {}", e.getMessage());
            throw new CbsServiceException("CBS service unavailable.");
        }
    }

    private JsonNode requireSuccessResponse(JsonNode response) {
        if (response != null && response.has("status") && "FAILURE".equalsIgnoreCase(response.get("status").asText())) {
            String message = response.has("message") ? response.get("message").asText() : "CBS request failed";
            throw new CbsServiceException(message);
        }
        return response;
    }

    private CbsServiceException toCbsServiceException(HttpClientErrorException e) {
        try {
            JsonNode body = objectMapper.readTree(e.getResponseBodyAsString());
            if (body.has("message")) {
                return new CbsServiceException(body.get("message").asText());
            }
        } catch (Exception ignored) {
            // fall through to generic message
        }
        return new CbsServiceException("CBS request failed with status " + e.getStatusCode());
    }

    private JsonNode parseSafe(String json) {
        try { return objectMapper.readTree(json); }
        catch (Exception e) { return objectMapper.createObjectNode(); }
    }
}
