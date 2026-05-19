package com.idigicloud.accountopening.service;

import com.idigicloud.accountopening.dto.request.*;
import com.idigicloud.accountopening.dto.response.AccountOpeningResponse;

import java.util.List;

public interface AccountOpeningService {

    // 7-Step flow
    AccountOpeningResponse initiateNewAccount(Long userId, NewAccountRequest request);           // Step 1
    AccountOpeningResponse selectProduct(ProductSelectionRequest request);                       // Step 2
    AccountOpeningResponse setRelationship(RelationshipRequest request);                         // Step 3
    AccountOpeningResponse uploadDocument(DocumentUploadRequest request);                        // Step 4
    AccountOpeningResponse saveBasicDetails(BasicDetailsRequest request);                        // Step 5
    AccountOpeningResponse addNominees(NomineeRequest request);                                  // Step 7
    AccountOpeningResponse submitApplication(Long accountOpeningRequestId);                      // Submit

    // Post-submit
    AccountOpeningResponse applyInitialFunding(InitialFundingRequest request);                   // Step 8
    AccountOpeningResponse issueWelcomeKit(Long accountOpeningRequestId);                        // Step 9

    // Queries
    AccountOpeningResponse getById(Long id);
    List<AccountOpeningResponse> getByUserId(Long userId);
    List<AccountOpeningResponse> getAllApplications();
}
