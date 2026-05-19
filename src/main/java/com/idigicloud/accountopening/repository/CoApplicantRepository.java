package com.idigicloud.accountopening.repository;

import com.idigicloud.accountopening.entity.CoApplicant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CoApplicantRepository extends JpaRepository<CoApplicant, Long> {
    List<CoApplicant> findByAccountOpeningRequestId(Long requestId);
}
