package com.connto.backend.repository;

import com.connto.backend.domain.ConnectionRequest;
import com.connto.backend.domain.ConnectionRequestStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConnectionRequestRepository extends JpaRepository<ConnectionRequest, UUID> {

    Optional<ConnectionRequest> findByFromUserIdAndToUserId(UUID fromId, UUID toId);

    List<ConnectionRequest> findByToUserIdAndStatusOrderByCreatedAtDesc(
            UUID toUserId, ConnectionRequestStatus status);

    List<ConnectionRequest> findByFromUserIdAndStatusOrderByCreatedAtDesc(
            UUID fromUserId, ConnectionRequestStatus status);
}
