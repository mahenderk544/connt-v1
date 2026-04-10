package com.connto.backend.repository;

import com.connto.backend.domain.WalletLedgerEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletLedgerEntryRepository extends JpaRepository<WalletLedgerEntry, UUID> {

    List<WalletLedgerEntry> findTop100ByUser_IdOrderByCreatedAtDesc(UUID userId);
}
