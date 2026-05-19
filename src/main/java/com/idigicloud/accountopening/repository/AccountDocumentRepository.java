package com.idigicloud.accountopening.repository;

import com.idigicloud.accountopening.entity.AccountDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AccountDocumentRepository extends JpaRepository<AccountDocument, Long> {
    List<AccountDocument> findByAccountOpeningRequestId(Long requestId);
}
