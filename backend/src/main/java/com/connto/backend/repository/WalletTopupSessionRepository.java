package com.connto.backend.repository;

import com.connto.backend.domain.WalletTopupSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTopupSessionRepository extends JpaRepository<WalletTopupSession, UUID> {

    Optional<WalletTopupSession> findByIdAndUserId(UUID id, UUID userId);
}
