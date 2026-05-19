package com.idigicloud.accountopening.controller;

import com.idigicloud.accountopening.cbs.CbsClient;
import com.idigicloud.accountopening.dto.response.ApiResponse;
import com.idigicloud.accountopening.exception.ResourceNotFoundException;
import com.idigicloud.accountopening.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Bridge controller that maps the exact API paths the Next.js frontend uses
 * (as defined in src/services/accountApi.ts) to the real backend services.
 *
 * Frontend API calls (from accountApi.ts):
 *   GET  /customer/search
 *   GET  /products
 *   POST /account/create
 *   POST /document/upload
 *   POST /nominee
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")

@Tag(name = "UI Bridge APIs", description = "Exact endpoint paths matching the Next.js frontend (accountApi.ts)")
public class UiBridgeController {

    private final CbsClient cbsClient;
    private final UserRepository userRepository;

    // ─────────────────────────────────────────────────────────
    //  GET /customer/search
    //  Frontend: searchCustomer({ customerType, searchBy, searchValue })
    // ─────────────────────────────────────────────────────────
    @GetMapping("/customer/search")
    @Operation(
        summary     = "Search customer (UI endpoint)",
        description = "Matches frontend accountApi.ts: GET /customer/search?customerType=&searchBy=&searchValue=. " +
                      "Delegates to CBS mock for customer lookup."
    )
    public ResponseEntity<ApiResponse<JsonNode>> searchCustomer(
            @RequestParam(required = false) String customerType,
            @RequestParam(required = false) String searchBy,
            @RequestParam(required = false) String searchValue,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("UI /customer/search => customerType={}, searchBy={}, searchValue={}",
                customerType, searchBy, searchValue);

        if (searchValue == null || searchValue.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success("Please provide a search value", null));
        }

        // Map frontend searchBy values to CBS searchBy values
        String cbsSearchBy = mapSearchBy(searchBy);

        try {
            JsonNode cbsResult = cbsClient.searchCustomer(cbsSearchBy, searchValue);
            return ResponseEntity.ok(ApiResponse.success("Customer found", cbsResult));
        } catch (Exception e) {
            log.warn("CBS customer search failed: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.failure("No customer found with " + searchBy + ": " + searchValue));
        }
    }

    // ─────────────────────────────────────────────────────────
    //  GET /products
    //  Frontend: getProducts()
    // ─────────────────────────────────────────────────────────
    @GetMapping("/products")
    @Operation(
        summary     = "Get available products (UI endpoint)",
        description = "Matches frontend accountApi.ts: GET /products. Returns available account products/offers."
    )
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getProducts() {
        log.info("UI /products => returning product catalog");

        // Product catalog — in a real system this would come from CBS or a product DB
        List<Map<String, Object>> products = List.of(
            Map.of("productCode", "CPSAVIUTAOO1", "productName", "Regular Savings Account",
                   "accountType", "Savings", "productClass", "CASA", "productGroup", "PGSAVIUTAOO1",
                   "offerCode", "OFFERSAVIUTAOO1", "offerName", "SAVINGS OFFER",
                   "currency", "INR", "totalFees", 5),
            Map.of("productCode", "CPCURUTAOO1", "productName", "Business Current Account",
                   "accountType", "Current", "productClass", "CASA", "productGroup", "PGCURUTAOO1",
                   "offerCode", "OFFERCURUTAOO1", "offerName", "CURRENT OFFER",
                   "currency", "INR", "totalFees", 10),
            Map.of("productCode", "CPTDUTAOO1", "productName", "Term Deposit",
                   "accountType", "TD", "productClass", "TD", "productGroup", "PGTDUTAOO1",
                   "offerCode", "OFFERTDUTAOO1", "offerName", "TERM DEPOSIT OFFER",
                   "currency", "INR", "totalFees", 0),
            Map.of("productCode", "CPRDUTAOO1", "productName", "Recurring Deposit",
                   "accountType", "RD", "productClass", "RD", "productGroup", "PGRDUTAOO1",
                   "offerCode", "OFFERRDUTAOO1", "offerName", "RD OFFER",
                   "currency", "INR", "totalFees", 0)
        );
        return ResponseEntity.ok(ApiResponse.success("Products fetched", products));
    }

    // ─────────────────────────────────────────────────────────
    //  POST /account/create
    //  Frontend: createAccount(accountData)
    // ─────────────────────────────────────────────────────────
    @PostMapping("/account/create")
    @Operation(
        summary     = "Create account — full payload (UI endpoint)",
        description = "Matches frontend accountApi.ts: POST /account/create. " +
                      "Accepts the full AccountOpeningState from Redux store and creates account in CBS."
    )
    public ResponseEntity<ApiResponse<JsonNode>> createAccount(
            @RequestBody Map<String, Object> accountData,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("UI /account/create => customerId={}", accountData.get("customerId"));

        // Extract CBS payload from full Redux state
        String customerId = getStr(accountData, "customerId");
        String branchCode = getStr(accountData, "branchCode");
        String accountType = getStr(accountData, "accountType");
        String modeOfOp   = getStr(accountData, "modeOfOperation");

        if (customerId == null || customerId.isBlank()) {
            return ResponseEntity.ok(ApiResponse.failure("customerId is required"));
        }

        try {
            // Open account in CBS
            Map<String, Object> cbsPayload = Map.of(
                    "customerId",      customerId,
                    "accountType",     accountType != null ? accountType : "CASA",
                    "branchCode",      branchCode != null ? branchCode : "UTIBOO134",
                    "bankCode",        "AXB",
                    "currencyCode",    "INR",
                    "modeOfOperation", modeOfOp != null ? modeOfOp : "SINGLE"
            );
            JsonNode cbsResponse = cbsClient.openAccount(cbsPayload);
            return ResponseEntity.ok(ApiResponse.success("Account created successfully", cbsResponse));
        } catch (Exception e) {
            log.error("Account creation failed: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.failure("Account creation failed: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────
    //  POST /document/upload
    //  Frontend: uploadDocument(formData)  multipart/form-data
    // ─────────────────────────────────────────────────────────
    @PostMapping(value = "/document/upload", consumes = {"multipart/form-data", "application/json"})
    @Operation(
        summary     = "Upload KYC document (UI endpoint)",
        description = "Matches frontend accountApi.ts: POST /document/upload (multipart). " +
                      "Records the document reference and returns a documentId."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadDocument(
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String documentCategory,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String accountOpeningId) {

        log.info("UI /document/upload => documentType={}, customerId={}", documentType, customerId);

        // In production: save the multipart file, call Python OCR service, return documentId
        // For now: return a CBS-style document reference
        String documentId = "DOC-" + System.currentTimeMillis();

        Map<String, Object> result = Map.of(
                "documentId",       documentId,
                "documentType",     documentType != null ? documentType : "UNKNOWN",
                "documentCategory", documentCategory != null ? documentCategory : "ID_PROOF",
                "indexCategory",    "CUSTOMER",
                "versionNo",        1,
                "status",           "UPLOADED",
                "message",          "Document uploaded successfully. OCR processing will begin shortly."
        );
        return ResponseEntity.ok(ApiResponse.success("Document uploaded", result));
    }

    // ─────────────────────────────────────────────────────────
    //  POST /nominee
    //  Frontend: addNominee(nomineeData)
    // ─────────────────────────────────────────────────────────
    @PostMapping("/nominee")
    @Operation(
        summary     = "Add nominee (UI endpoint)",
        description = "Matches frontend accountApi.ts: POST /nominee. " +
                      "Adds nominee to CBS for the given account number."
    )
    public ResponseEntity<ApiResponse<JsonNode>> addNominee(
            @RequestBody Map<String, Object> nomineeData) {

        log.info("UI /nominee => accountNumber={}", nomineeData.get("accountNumber"));

        try {
            JsonNode cbsResponse = cbsClient.addNominee(nomineeData);
            return ResponseEntity.ok(ApiResponse.success("Nominee added successfully", cbsResponse));
        } catch (Exception e) {
            log.error("Add nominee failed: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.failure("Failed to add nominee: " + e.getMessage()));
        }
    }

    // ─── Private Helpers ──────────────────────────────────────

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    /**
     * Maps frontend "searchBy" dropdown values to CBS searchBy parameter values.
     * Frontend uses: "Customer Id", "Mobile Number", "Email", "Aadhaar", "PAN"
     * CBS uses:      "CUSTOMER_ID", "MOBILE_NUMBER", "EMAIL", "AADHAAR", "PAN"
     */
    private String mapSearchBy(String frontendSearchBy) {
        if (frontendSearchBy == null) return "CUSTOMER_ID";
        return switch (frontendSearchBy.toLowerCase().replace(" ", "_")) {
            case "customer_id"   -> "CUSTOMER_ID";
            case "mobile_number",
                 "mobile"        -> "MOBILE_NUMBER";
            case "email"         -> "EMAIL";
            case "aadhaar"       -> "AADHAAR";
            case "pan"           -> "PAN";
            default              -> "CUSTOMER_ID";
        };
    }
}
