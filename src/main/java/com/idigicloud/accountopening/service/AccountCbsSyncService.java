package com.idigicloud.accountopening.service;

import com.idigicloud.accountopening.exception.ResourceNotFoundException;
import com.idigicloud.accountopening.repository.AccountOpeningRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists CBS account assignment in its own transaction so a later activation
 * failure does not roll back the assigned account number.
 */
@Service
@RequiredArgsConstructor
public class AccountCbsSyncService {

    private final AccountOpeningRequestRepository requestRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistCbsAccountNumber(Long accountOpeningRequestId, String cbsAccountNumber) {
        var opening = requestRepository.findById(accountOpeningRequestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account opening request not found: " + accountOpeningRequestId));
        opening.setCbsAccountNumber(cbsAccountNumber);
        requestRepository.save(opening);
    }
}
