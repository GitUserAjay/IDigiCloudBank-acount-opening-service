package com.idigicloud.accountopening.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.idigicloud.accountopening.cbs.CbsServiceException;
import com.idigicloud.accountopening.cbs.CbsClient;
import com.idigicloud.accountopening.dto.request.*;
import com.idigicloud.accountopening.dto.response.AccountOpeningResponse;
import com.idigicloud.accountopening.entity.*;
import com.idigicloud.accountopening.enums.AccountOpeningStatus;
import com.idigicloud.accountopening.enums.ProductClass;
import com.idigicloud.accountopening.exception.InvalidOperationException;
import com.idigicloud.accountopening.exception.ResourceNotFoundException;
import com.idigicloud.accountopening.repository.*;
import com.idigicloud.accountopening.service.AccountCbsSyncService;
import com.idigicloud.accountopening.service.AccountOpeningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountOpeningServiceImpl implements AccountOpeningService {

    private final AccountOpeningRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final NomineeRepository nomineeRepository;
    private final CoApplicantRepository coApplicantRepository;
    private final AccountDocumentRepository documentRepository;
    private final CbsClient cbsClient;
    private final AccountCbsSyncService accountCbsSyncService;

    // ─────────────────────────────────────────────────────────
    //  STEP 1 — Initiate New Account
    // ─────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AccountOpeningResponse initiateNewAccount(Long userId, NewAccountRequest request) {
        log.info("Step 1: Initiating account opening for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (user.getCbsCustomerId() == null) {
            throw new InvalidOperationException(
                    "User is not registered in CBS. Please contact support or re-register.");
        }

        AccountOpeningRequest opening = AccountOpeningRequest.builder()
                .productClass(request.getProductClass())
                .customerType(request.getCustomerType())
                .branchCode(request.getBranchCode() != null ? request.getBranchCode() : (user.getBranchCode() != null ? user.getBranchCode() : "UTIBOO134"))
                .bankCode("AXB")
                .currencyCode(request.getCurrencyCode())
                .cbsCustomerId(user.getCbsCustomerId())
                .status(AccountOpeningStatus.DRAFT)
                .currentStep(1)
                .user(user)
                .build();

        return mapToResponse(requestRepository.save(opening));
    }

    // ─────────────────────────────────────────────────────────
    //  STEP 2 — Product Selection
    // ─────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AccountOpeningResponse selectProduct(ProductSelectionRequest request) {
        log.info("Step 2: Product selection for requestId={}", request.getAccountOpeningRequestId());

        AccountOpeningRequest opening = findById(request.getAccountOpeningRequestId());
        validateStep(opening, 1, "Product selection requires Step 1 to be completed");

        opening.setOfferCode(request.getOfferCode());
        opening.setOfferName(request.getOfferName());
        opening.setProductCode(request.getProductCode());
        opening.setAccountType(request.getAccountType());
        opening.setProductGroup(request.getProductGroup());
        opening.setTotalFees(request.getTotalFees());
        opening.setStatus(AccountOpeningStatus.PRODUCT_SELECTED);
        opening.setCurrentStep(2);

        return mapToResponse(requestRepository.save(opening));
    }

    // ─────────────────────────────────────────────────────────
    //  STEP 3 — Relationship (Account Number + Mode of Operation)
    // ─────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AccountOpeningResponse setRelationship(RelationshipRequest request) {
        log.info("Step 3: Setting relationship for requestId={}", request.getAccountOpeningRequestId());

        AccountOpeningRequest opening = findById(request.getAccountOpeningRequestId());
        validateStep(opening, 2, "Relationship setup requires Step 2 to be completed");

        opening.setModeOfOperation(request.getModeOfOperation());
        opening.setStatus(AccountOpeningStatus.RELATIONSHIP_SET);
        opening.setCurrentStep(3);

        // Save co-applicants
        if (request.getCoApplicants() != null && !request.getCoApplicants().isEmpty()) {
            List<CoApplicant> coApplicants = request.getCoApplicants().stream()
                    .map(ca -> CoApplicant.builder()
                            .cbsCustomerId(ca.getCbsCustomerId())
                            .customerName(ca.getCustomerName())
                            .customerRole(ca.getCustomerRole())
                            .isExistingCustomer(ca.isExistingCustomer())
                            .accountOpeningRequest(opening)
                            .build())
                    .collect(Collectors.toList());
            coApplicantRepository.saveAll(coApplicants);
        }

        return mapToResponse(requestRepository.save(opening));
    }

    // ─────────────────────────────────────────────────────────
    //  STEP 4 — Associated Documents
    // ─────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AccountOpeningResponse uploadDocument(DocumentUploadRequest request) {
        log.info("Step 4: Document upload for requestId={}", request.getAccountOpeningRequestId());

        AccountOpeningRequest opening = findById(request.getAccountOpeningRequestId());

        AccountDocument document = AccountDocument.builder()
                .documentType(request.getDocumentType())
                .documentCategory(request.getDocumentCategory())
                .fileName(request.getFileName())
                .filePath(request.getFilePath())
                .documentId(request.getDocumentId())
                .versionNo(request.getVersionNo() != null ? request.getVersionNo() : 1)
                .indexCategory(request.getIndexCategory() != null ? request.getIndexCategory() : "CUSTOMER")
                .isVerified(false)
                .accountOpeningRequest(opening)
                .build();

        documentRepository.save(document);

        opening.setStatus(AccountOpeningStatus.DOCUMENTS_UPLOADED);
        opening.setCurrentStep(4);

        return mapToResponse(requestRepository.save(opening));
    }

    // ─────────────────────────────────────────────────────────
    //  STEP 5 — Basic Details
    // ─────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AccountOpeningResponse saveBasicDetails(BasicDetailsRequest request) {
        log.info("Step 5: Basic details for requestId={}", request.getAccountOpeningRequestId());

        AccountOpeningRequest opening = findById(request.getAccountOpeningRequestId());

        opening.setAccountName(request.getAccountName());
        opening.setDebitCardVariant(request.getDebitCardVariant());
        opening.setStatus(AccountOpeningStatus.BASIC_DETAILS_DONE);
        opening.setCurrentStep(5);

        return mapToResponse(requestRepository.save(opening));
    }

    // ─────────────────────────────────────────────────────────
    //  STEP 7 — Nominee Details
    // ─────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AccountOpeningResponse addNominees(NomineeRequest request) {
        log.info("Step 7: Adding nominees for requestId={}", request.getAccountOpeningRequestId());

        AccountOpeningRequest opening = findById(request.getAccountOpeningRequestId());

        // Validate total share = 100
        int totalShare = request.getNominees().stream()
                .mapToInt(NomineeRequest.NomineeDetail::getSharePercentage).sum();
        if (totalShare != 100) {
            throw new InvalidOperationException("Total nominee share percentage must equal 100. Current: " + totalShare);
        }

        request.getNominees().forEach(detail -> {
            Nominee nominee = Nominee.builder()
                    .firstName(detail.getFirstName())
                    .middleName(detail.getMiddleName())
                    .lastName(detail.getLastName())
                    .gender(detail.getGender())
                    .dateOfBirth(parseFlexibleDate(detail.getDateOfBirth()))
                    .relationship(detail.getRelationship())
                    .sharePercentage(detail.getSharePercentage())
                    .mobileNumber(detail.getMobileNumber())
                    .email(detail.getEmail())
                    .addressLine1(detail.getAddressLine1())
                    .addressLine2(detail.getAddressLine2())
                    .country(detail.getCountry())
                    .postalCode(detail.getPostalCode())
                    .accountOpeningRequest(opening)
                    .build();
            nomineeRepository.save(nominee);

            // Sync nominee to CBS
            if (opening.getCbsAccountNumber() != null) {
                syncNomineeToCbs(opening.getCbsAccountNumber(), detail);
            }
        });

        opening.setStatus(AccountOpeningStatus.NOMINEE_ADDED);
        opening.setCurrentStep(7);

        return mapToResponse(requestRepository.save(opening));
    }

    // ─────────────────────────────────────────────────────────
    //  SUBMIT — Final submission to CBS
    // ─────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AccountOpeningResponse submitApplication(Long accountOpeningRequestId) {
        log.info("Submitting application requestId={}", accountOpeningRequestId);

        AccountOpeningRequest opening = findById(accountOpeningRequestId);

        if (opening.getStatus() == AccountOpeningStatus.SUBMITTED && opening.getCurrentStep() != null
                && opening.getCurrentStep() >= 8) {
            log.info("Application requestId={} already submitted", accountOpeningRequestId);
            return mapToResponse(opening);
        }

        ensureCbsAccountExists(opening);
        opening = findById(accountOpeningRequestId);

        cbsClient.activateAccount(opening.getCbsAccountNumber());
        log.info("CBS account activated: {}", opening.getCbsAccountNumber());

        opening.setStatus(AccountOpeningStatus.SUBMITTED);
        // Do NOT overwrite currentStep here — nominees already set it to 7.
        // Step numbers: 1-7 are the wizard steps; 8 = SUBMITTED terminal state.
        opening.setCurrentStep(8);
        return mapToResponse(requestRepository.save(opening));
    }

    // ─────────────────────────────────────────────────────────
    //  STEP 8 — Initial Funding
    // ─────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AccountOpeningResponse applyInitialFunding(InitialFundingRequest request) {
        log.info("Step 8: Initial funding for requestId={}", request.getAccountOpeningRequestId());

        AccountOpeningRequest opening = findById(request.getAccountOpeningRequestId());

        ensureCbsAccountExists(opening);

        // Update CBS
        Map<String, Object> payload = new HashMap<>();
        payload.put("accountNumber", opening.getCbsAccountNumber());
        payload.put("amount", request.getAmount());
        payload.put("fundingMode", request.getFundingMode());
        try {
            cbsClient.updateInitialFunding(payload);
        } catch (Exception e) {
            log.warn("CBS initial funding sync failed (amount saved locally): {}", e.getMessage());
            throw new CbsServiceException("CBS initial funding update failed. Please retry.");
        }

        opening.setInitialFundingAmount(request.getAmount());
        opening.setFundingMode(request.getFundingMode());
        opening.setStatus(AccountOpeningStatus.SUBMITTED);

        return mapToResponse(requestRepository.save(opening));
    }

    // ─────────────────────────────────────────────────────────
    //  STEP 9 — Issue Welcome Kit
    // ─────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AccountOpeningResponse issueWelcomeKit(Long accountOpeningRequestId) {
        log.info("Step 9: Issuing welcome kit for requestId={}", accountOpeningRequestId);

        AccountOpeningRequest opening = findById(accountOpeningRequestId);

        if (opening.getCbsAccountNumber() == null) {
            throw new InvalidOperationException("No CBS account number found.");
        }

        JsonNode cbsResponse = cbsClient.issueWelcomeKit(opening.getCbsAccountNumber());
        if (cbsResponse.has("data") && cbsResponse.get("data").has("welcomeKitReference")) {
            opening.setWelcomeKitReference(cbsResponse.get("data").get("welcomeKitReference").asText());
        }

        opening.setStatus(AccountOpeningStatus.ACTIVE);
        return mapToResponse(requestRepository.save(opening));
    }

    // ─────────────────────────────────────────────────────────
    //  QUERIES
    // ─────────────────────────────────────────────────────────
    @Override
    public AccountOpeningResponse getById(Long id) {
        return mapToResponse(findById(id));
    }

    @Override
    public List<AccountOpeningResponse> getByUserId(Long userId) {
        return requestRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<AccountOpeningResponse> getAllApplications() {
        return requestRepository.findAll()
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────

    private AccountOpeningRequest findById(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account opening request not found: " + id));
    }

    private void validateStep(AccountOpeningRequest opening, int expectedStep, String message) {
        if (opening.getCurrentStep() == null || opening.getCurrentStep() < expectedStep) {
            throw new InvalidOperationException(message);
        }
    }

    private void ensureCbsAccountExists(AccountOpeningRequest opening) {
        AccountOpeningRequest latest = findById(opening.getId());
        if (latest.getCbsAccountNumber() != null && !latest.getCbsAccountNumber().isBlank()) {
            opening.setCbsAccountNumber(latest.getCbsAccountNumber());
            return;
        }
        if (latest.getModeOfOperation() == null || latest.getModeOfOperation().isBlank()) {
            throw new InvalidOperationException("Mode of operation is required. Complete Step 3 (Relationship) first.");
        }

        String cbsAccountNumber = openAccountInCbs(latest, latest.getModeOfOperation());
        accountCbsSyncService.persistCbsAccountNumber(latest.getId(), cbsAccountNumber);
        opening.setCbsAccountNumber(cbsAccountNumber);
        syncSavedNomineesToCbs(cbsAccountNumber, latest.getId());
        log.info("CBS account {} assigned for requestId={}", cbsAccountNumber, latest.getId());
    }

    private String openAccountInCbs(AccountOpeningRequest opening, String modeOfOperation) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("customerId", opening.getCbsCustomerId());
            payload.put("accountType", mapProductClassToCbsAccountType(opening.getProductClass()));
            payload.put("offerCode", opening.getOfferCode());
            payload.put("productCode", opening.getProductCode());
            payload.put("branchCode", opening.getBranchCode());
            payload.put("bankCode", opening.getBankCode());
            payload.put("currencyCode", opening.getCurrencyCode());
            payload.put("modeOfOperation", modeOfOperation);

            JsonNode response = cbsClient.openAccount(payload);
            if (response.has("data") && response.get("data").has("accountNumber")) {
                String accountNumber = response.get("data").get("accountNumber").asText();
                if (accountNumber == null || accountNumber.isBlank()) {
                    throw new InvalidOperationException(
                            "CBS returned an empty account number. Check CBS service configuration.");
                }
                return accountNumber;
            }
            // CBS returned 200 but the expected accountNumber field is missing
            throw new InvalidOperationException(
                    "CBS accepted the request but returned no account number in response. " +
                            "Please ensure CBS service is running and properly configured.");
        } catch (CbsServiceException | InvalidOperationException e) {
            throw e;
        } catch (Exception e) {
            log.error("CBS openAccount call failed: {}", e.getMessage());
            throw new CbsServiceException("CBS account creation failed. Please retry.");
        }
    }

    private void syncNomineeToCbs(String accountNumber, NomineeRequest.NomineeDetail detail) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("accountNumber", accountNumber);
            payload.put("firstName", detail.getFirstName());
            payload.put("middleName", detail.getMiddleName());
            payload.put("lastName", detail.getLastName());
            payload.put("gender", detail.getGender());
            payload.put("dateOfBirth", parseFlexibleDate(detail.getDateOfBirth()).format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")));
            payload.put("relationship", detail.getRelationship());
            payload.put("sharePercentage", detail.getSharePercentage());
            payload.put("mobileNumber", detail.getMobileNumber());
            payload.put("email", detail.getEmail());
            payload.put("addressLine1", detail.getAddressLine1());
            payload.put("country", detail.getCountry());
            payload.put("postalCode", detail.getPostalCode());
            cbsClient.addNominee(payload);
        } catch (Exception e) {
            log.warn("CBS nominee sync failed: {}", e.getMessage());
        }
    }

    private void syncSavedNomineesToCbs(String accountNumber, Long accountOpeningRequestId) {
        List<Nominee> nominees = nomineeRepository.findByAccountOpeningRequestId(accountOpeningRequestId);
        for (Nominee nominee : nominees) {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("accountNumber", accountNumber);
                payload.put("firstName", nominee.getFirstName());
                payload.put("middleName", nominee.getMiddleName());
                payload.put("lastName", nominee.getLastName());
                payload.put("gender", nominee.getGender());
                payload.put("dateOfBirth", nominee.getDateOfBirth() != null
                        ? nominee.getDateOfBirth().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"))
                        : null);
                payload.put("relationship", nominee.getRelationship());
                payload.put("sharePercentage", nominee.getSharePercentage());
                payload.put("mobileNumber", nominee.getMobileNumber());
                payload.put("email", nominee.getEmail());
                payload.put("addressLine1", nominee.getAddressLine1());
                payload.put("country", nominee.getCountry());
                payload.put("postalCode", nominee.getPostalCode());
                cbsClient.addNominee(payload);
            } catch (Exception ex) {
                log.warn("CBS nominee sync failed for nomineeId={}: {}", nominee.getId(), ex.getMessage());
            }
        }
    }

    private AccountOpeningResponse mapToResponse(AccountOpeningRequest r) {
        List<AccountOpeningResponse.CoApplicantResponse> coApplicants =
                coApplicantRepository.findByAccountOpeningRequestId(r.getId())
                        .stream().map(c -> AccountOpeningResponse.CoApplicantResponse.builder()
                                .id(c.getId()).cbsCustomerId(c.getCbsCustomerId())
                                .customerName(c.getCustomerName()).customerRole(c.getCustomerRole())
                                .existingCustomer(c.isExistingCustomer()).build())
                        .collect(Collectors.toList());

        List<AccountOpeningResponse.NomineeResponse> nominees =
                nomineeRepository.findByAccountOpeningRequestId(r.getId())
                        .stream().map(n -> AccountOpeningResponse.NomineeResponse.builder()
                                .id(n.getId()).firstName(n.getFirstName()).middleName(n.getMiddleName())
                                .lastName(n.getLastName()).gender(n.getGender())
                                .dateOfBirth(n.getDateOfBirth() != null ? n.getDateOfBirth().toString() : null)
                                .relationship(n.getRelationship()).sharePercentage(n.getSharePercentage())
                                .mobileNumber(n.getMobileNumber()).email(n.getEmail()).build())
                        .collect(Collectors.toList());

        return AccountOpeningResponse.builder()
                .id(r.getId()).productClass(r.getProductClass()).customerType(r.getCustomerType())
                .branchCode(r.getBranchCode()).bankCode(r.getBankCode()).currencyCode(r.getCurrencyCode())
                .offerCode(r.getOfferCode()).offerName(r.getOfferName()).productCode(r.getProductCode())
                .accountType(r.getAccountType()).totalFees(r.getTotalFees())
                .cbsAccountNumber(r.getCbsAccountNumber()).modeOfOperation(r.getModeOfOperation())
                .coApplicants(coApplicants).accountName(r.getAccountName())
                .debitCardVariant(r.getDebitCardVariant()).nominees(nominees)
                .initialFundingAmount(r.getInitialFundingAmount()).fundingMode(r.getFundingMode())
                .cbsCustomerId(r.getCbsCustomerId()).welcomeKitReference(r.getWelcomeKitReference())
                .status(r.getStatus()).currentStep(r.getCurrentStep())
                .createdAt(r.getCreatedAt()).updatedAt(r.getUpdatedAt())
                .build();
    }
    private String mapProductClassToCbsAccountType(ProductClass productClass) {
        if (productClass == null) return "CASA";
        return switch (productClass) {
            case CASA -> "CASA";
            case TD   -> "TD";
            case RD   -> "RD";
            case LOAN -> "CASA";
        };
    }

    private LocalDate parseFlexibleDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            throw new InvalidOperationException("Nominee date of birth is required");
        }
        try { return LocalDate.parse(dateStr); } catch (Exception ignored) {}
        try { return LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy")); } catch (Exception ignored) {}
        try { return LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern("d-MMM-yyyy")); } catch (Exception ignored) {}
        try { return LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")); } catch (Exception ignored) {}
        throw new InvalidOperationException("Cannot parse nominee date of birth: '" + dateStr + "'. Expected YYYY-MM-DD");
    }


}
