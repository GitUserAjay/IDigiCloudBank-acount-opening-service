package com.idigicloud.accountopening.repository;

import com.idigicloud.accountopening.entity.Nominee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NomineeRepository extends JpaRepository<Nominee, Long> {
    List<Nominee> findByAccountOpeningRequestId(Long requestId);
}
