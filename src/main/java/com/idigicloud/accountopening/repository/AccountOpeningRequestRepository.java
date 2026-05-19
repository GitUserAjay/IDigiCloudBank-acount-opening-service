package com.idigicloud.accountopening.repository;

import com.idigicloud.accountopening.entity.AccountOpeningRequest;
import com.idigicloud.accountopening.enums.AccountOpeningStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountOpeningRequestRepository extends JpaRepository<AccountOpeningRequest, Long> {
    List<AccountOpeningRequest> findByUserId(Long userId);
    List<AccountOpeningRequest> findByStatus(AccountOpeningStatus status);
    Optional<AccountOpeningRequest> findByCbsAccountNumber(String cbsAccountNumber);
    List<AccountOpeningRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
}
