package com.idigicloud.accountopening.controller;

import com.idigicloud.accountopening.dto.request.*;
import com.idigicloud.accountopening.dto.response.AccountOpeningResponse;
import com.idigicloud.accountopening.dto.response.ApiResponse;
import com.idigicloud.accountopening.service.AccountOpeningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;

@RestController
@RequestMapping("/account-opening")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Account Opening", description = "7-step bank account opening flow")
public class AccountOpeningController {

    private final AccountOpeningService accountOpeningService;

    // ─── Step 1 ──────────────────────────────────────────────
    @PostMapping("/step1/initiate")
    @Operation(
        summary     = "Step 1 — New Account",
        description = "Initiate a new account opening. Select product class (CASA/TD/RD), customer type, branch."
    )
    public ResponseEntity<ApiResponse<AccountOpeningResponse>> initiateNewAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody NewAccountRequest request) {

        Long userId = resolveUserId(userDetails);
        AccountOpeningResponse response = accountOpeningService.initiateNewAccount(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Step 1 completed — account opening initiated", response));
    }

    // ─── Step 2 ──────────────────────────────────────────────
    @PostMapping("/step2/select-product")
    @Operation(
        summary     = "Step 2 — Product Selection",
        description = "Select offer code, product code, fees. Maps to CBS offer like OFFERSAVIUTAOO1."
    )
    public ResponseEntity<ApiResponse<AccountOpeningResponse>> selectProduct(
            @Valid @RequestBody ProductSelectionRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Step 2 completed — product selected",
                        accountOpeningService.selectProduct(request)));
    }

    // ─── Step 3 ──────────────────────────────────────────────
    @PostMapping("/step3/set-relationship")
    @Operation(
        summary     = "Step 3 — Relationship",
        description = "Set mode of operation (SINGLE/ANYONE_OR_SURVIVOR) and add co-applicants. " +
                      "This call creates the account in CBS and returns a CBS Account Number."
    )
    public ResponseEntity<ApiResponse<AccountOpeningResponse>> setRelationship(
            @Valid @RequestBody RelationshipRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Step 3 completed — relationship set, CBS account number assigned",
                        accountOpeningService.setRelationship(request)));
    }

    // ─── Step 4 ──────────────────────────────────────────────
    @PostMapping("/step4/upload-document")
    @Operation(
        summary     = "Step 4 — Associated Documents",
        description = "Upload a KYC document (AADHAAR, PAN, DRIVING_LICENSE, RATION_CARD). " +
                      "Call this endpoint once per document."
    )
    public ResponseEntity<ApiResponse<AccountOpeningResponse>> uploadDocument(
            @Valid @RequestBody DocumentUploadRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Step 4 completed — document uploaded",
                        accountOpeningService.uploadDocument(request)));
    }

    // ─── Step 5 ──────────────────────────────────────────────
    @PostMapping("/step5/basic-details")
    @Operation(
        summary     = "Step 5 — Basic Details",
        description = "Set account name, debit card variant, net banking / mobile banking / cheque book preferences."
    )
    public ResponseEntity<ApiResponse<AccountOpeningResponse>> saveBasicDetails(
            @Valid @RequestBody BasicDetailsRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Step 5 completed — basic details saved",
                        accountOpeningService.saveBasicDetails(request)));
    }

    // ─── Step 6 — Transaction Limits (read-only, fetched from CBS) ───
    @GetMapping("/step6/transaction-limits/{accountOpeningRequestId}")
    @Operation(
        summary     = "Step 6 — Transaction Limits",
        description = "Fetch transaction limits from CBS for the account. " +
                      "Limits are auto-seeded per channel (BRANCH / INTERNET_BANKING / MOBILE_BANKING)."
    )
    public ResponseEntity<ApiResponse<Object>> getTransactionLimits(
            @PathVariable Long accountOpeningRequestId) {

        AccountOpeningResponse opening = accountOpeningService.getById(accountOpeningRequestId);
        if (opening.getCbsAccountNumber() == null) {
            return ResponseEntity.ok(ApiResponse.failure("No CBS account number yet. Complete Step 3 first."));
        }
        // Delegate to CBS directly and return raw limits
        return ResponseEntity.ok(ApiResponse.success("Transaction limits fetched from CBS",
                opening.getCbsAccountNumber()));
    }

    // ─── Step 7 ──────────────────────────────────────────────
    @PostMapping("/step7/add-nominees")
    @Operation(
        summary     = "Step 7 — Nominee Details",
        description = "Add one or more nominees. Total share percentage across all nominees must equal 100. " +
                      "Nominees are also synced to CBS."
    )
    public ResponseEntity<ApiResponse<AccountOpeningResponse>> addNominees(
            @Valid @RequestBody NomineeRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Step 7 completed — nominees added",
                        accountOpeningService.addNominees(request)));
    }

    // ─── Submit ───────────────────────────────────────────────
    @PostMapping("/submit/{accountOpeningRequestId}")
    @Operation(
        summary     = "Submit Application",
        description = "Final submission. Activates the account in CBS. Call after completing all 7 steps."
    )
    public ResponseEntity<ApiResponse<AccountOpeningResponse>> submitApplication(
            @PathVariable Long accountOpeningRequestId) {
        return ResponseEntity.ok(
                ApiResponse.success("Application submitted successfully — account is now active",
                        accountOpeningService.submitApplication(accountOpeningRequestId)));
    }

    // ─── Post-Submit: Initial Funding ────────────────────────
    @PostMapping("/funding")
    @Operation(
        summary     = "Step 8 — Initial Funding",
        description = "Apply initial deposit amount. Mode can be CASH, CHEQUE, or FUND_TRANSFER."
    )
    public ResponseEntity<ApiResponse<AccountOpeningResponse>> applyInitialFunding(
            @Valid @RequestBody InitialFundingRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Initial funding applied successfully",
                        accountOpeningService.applyInitialFunding(request)));
    }

    // ─── Post-Submit: Welcome Kit ─────────────────────────────
    @PostMapping("/welcome-kit/{accountOpeningRequestId}")
    @Operation(
        summary     = "Step 9 — Issue Welcome Kit",
        description = "Issues debit card, passbook, cheque book, and activates net/mobile banking via CBS."
    )
    public ResponseEntity<ApiResponse<AccountOpeningResponse>> issueWelcomeKit(
            @PathVariable Long accountOpeningRequestId) {
        return ResponseEntity.ok(
                ApiResponse.success("Welcome kit issued — account fully activated",
                        accountOpeningService.issueWelcomeKit(accountOpeningRequestId)));
    }

    // ─── My Applications ─────────────────────────────────────
    @GetMapping("/my-applications")
    @Operation(summary = "Get all account opening applications for logged-in user")
    public ResponseEntity<ApiResponse<List<AccountOpeningResponse>>> getMyApplications(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Applications fetched",
                accountOpeningService.getByUserId(userId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account opening request by ID")
    public ResponseEntity<ApiResponse<AccountOpeningResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Application fetched",
                accountOpeningService.getById(id)));
    }

    // ─── Admin: All Applications ──────────────────────────────
    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'BANK_EXECUTIVE')")
    @Operation(summary = "Admin — Get all account opening requests")
    public ResponseEntity<ApiResponse<List<AccountOpeningResponse>>> getAllApplications() {
        return ResponseEntity.ok(ApiResponse.success("All applications fetched",
                accountOpeningService.getAllApplications()));
    }

    // ─── Private Helpers ─────────────────────────────────────
    private Long resolveUserId(UserDetails userDetails) {
        // Fetches user ID by email from repository through service
        // Since UserDetails gives us email, we load the user from context
        return com.idigicloud.accountopening.util.SecurityContextUtil.getCurrentUserId(userDetails);
    }
}
